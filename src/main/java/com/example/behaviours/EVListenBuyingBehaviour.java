package com.example.behaviours;

import com.example.domain.Map;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.example.agents.EVAgent;

import java.util.*;

public class EVListenBuyingBehaviour extends CyclicBehaviour {
    /// NEGOTIATING BUYING SIDE
    private final EVAgent evAgent;
    private double tryCount = 0;
    private int negotiationRound;
    private int maxRounds;
    private double meanPrice;
    private double chargingUrgency;
    private double money;
    private double batteryRatio;
    private double bestUtility = 0;

    private String myName;

    public EVListenBuyingBehaviour(EVAgent evAgent) {
        this.evAgent = evAgent;
    }

    public void action() {
        // Logic for negotiating from the buyer perspective
        meanPrice = evAgent.getMeanPrice();
        chargingUrgency = evAgent.getChargingUrgency();
        money = evAgent.getTotalMoney();
        batteryRatio = evAgent.getBatteryRatio();
        myName = evAgent.getLocalName();
        negotiationRound = 1;
        maxRounds = (int)(chargingUrgency * 5); // HARDCODED MAX 5

        System.out.println(myAgent.getLocalName() + " starting negotiations");
        beginNegotiations();
    }

    private void beginNegotiations() {

        List<AID> allEVs = evAgent.getEvInQueue();
        List<java.util.Map.Entry<AID, Double>> counterBids = new ArrayList<>();
        int count = allEVs.size();
        double initialBid = generateInitialBid();

        // Send initial offer
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        msg.setContent(String.format(Locale.US,"%.2f", initialBid));
        for (AID ev : allEVs) {
            msg.addReceiver(ev);
        }
        evAgent.send(msg);


        long timeout = 3000;

        List<ACLMessage> replies = new ArrayList<>();
        List<java.util.Map.Entry<AID, Double>> oldBids = new ArrayList<>();
        List<java.util.Map.Entry<AID, Double>> newBids = new ArrayList<>();

        AID finalSeller = null;
        double finalPrice = Double.MAX_VALUE;

        // Main negotiation loop
        while (true) {

            long start = System.currentTimeMillis();

            MessageTemplate temp1 = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
            MessageTemplate template = MessageTemplate.or(temp1,
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

            // Wait for replies
            while (System.currentTimeMillis() - start < timeout) {
                ACLMessage reply = evAgent.receive(template);

                if (reply != null) {
                    replies.add(reply);
                    System.out.println("Received reply from " + reply.getSender().getLocalName() +
                            ": " + reply.getContent());
                } else {
                    block(100); // Pause a bit to avoid CPU overload
                }
            }

            // Finalizing negotiations
            if (negotiationRound == maxRounds) {

                // Collect proposals and decide which to buy
                double counterBid;
                for (ACLMessage reply : replies) {
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {

                        try {
                            counterBid = Double.parseDouble(reply.getContent().trim());
                            double utility = calculateUtility(counterBid);
                            if (utility > bestUtility) {
                                bestUtility = utility;
                                finalPrice = counterBid;
                                finalSeller = reply.getSender();
                            }

                        } catch (NumberFormatException e) {
                            System.out.println(evAgent.getLocalName()
                                    + " ERROR received bad message during negotiation");
                        }
                    }
                }

                // Send final replies
                ACLMessage finalYes = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                ACLMessage finalNo = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                for (ACLMessage reply : replies) {
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {

                        try {
                            counterBid = Double.parseDouble(reply.getContent());
                            if (reply.getSender() == finalSeller) {
                                finalYes.addReceiver(reply.getSender());
                            }
                            else {
                                finalNo.addReceiver(reply.getSender());
                            }

                        } catch (NumberFormatException e) {
                            System.out.println(evAgent.getLocalName()
                                    + " ERROR received bad message during negotiation");
                        }

                    }
                }
                evAgent.send(finalYes);
                evAgent.send(finalNo);
                break;
            }

            // Collect proposals and calculate next bids
            for (ACLMessage reply : replies) {
                if (reply.getPerformative() == ACLMessage.PROPOSE) {
                    double counterBid;
                    AID sender = reply.getSender();

                    try {
                        counterBid = Double.parseDouble(reply.getContent().trim());
                        counterBids.add(new AbstractMap.SimpleEntry<>(sender, counterBid));
                        if (negotiationRound == 1)
                            newBids.add(new AbstractMap.SimpleEntry<>(sender, generateNextBid(initialBid, counterBid)));
                        else
                            newBids.add(new AbstractMap.SimpleEntry<>(sender, generateNextBid(getValueByKey(oldBids, sender), counterBid)));

                    } catch (NumberFormatException e) {
                        System.out.println(evAgent.getLocalName()
                                + " ERROR received bad message during negotiation");
                    }
                }
                else if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {

                    // Price accepted by the seller EV
                    finalSeller = reply.getSender();
                    for (java.util.Map.Entry<AID, Double> entry : oldBids) {
                        if (entry.getKey().equals(finalSeller)) {
                            finalPrice = entry.getValue();
                        }
                    }
                    break;
                }
            }

            // Send proposals and rejection messages
            msg = new ACLMessage(ACLMessage.PROPOSE);
            ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            for (java.util.Map.Entry<AID, Double> entry : newBids) {
                if (entry.getValue() != -1) {
                    msg.setContent(String.format(Locale.US, "%s:%.2f", myName, entry.getValue()));
                    msg.addReceiver(entry.getKey());
                }
                else {
                    rejectMsg.addReceiver(entry.getKey());
                }
            }
            evAgent.send(msg);
            evAgent.send(rejectMsg);

            negotiationRound++;
            oldBids = newBids;
            newBids.clear();
            replies.clear();
        }



        if (finalPrice != Double.MAX_VALUE) {

            // Get info from seller EV about the spot and travel to it
            while (tryCount < 2) {
                MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage message = myAgent.blockingReceive(messageTemplate, 2000);

                if (message != null) {
                    String content = message.getContent();
                    String[] parts = content.split(":");
                    Map.Station station = Map.getStationByName(parts[0]);
                    evAgent.setCpId(parts[1]);

                    evAgent.travelToCp(station);
                    evAgent.removeBehaviour(this);
                    evAgent.addBehaviour(new EVListenSellingBehaviour(evAgent));
                    return;
                }
                tryCount++;
            }
        } else {

            // If all rejected then go back to asking CS's
            evAgent.setCurrentCommunication(evAgent.getCurrentLocation());
            evAgent.setCurrentCommunicationAid(new AID(evAgent.getCurrentCommunication().name(), AID.ISLOCALNAME));

            evAgent.sortStations(evAgent.getCurrentLocation());
            evAgent.removeBehaviour(this);
            evAgent.addBehaviour(new EVRequestCharging(evAgent));
        }

    }

    private double getValueByKey(List<java.util.Map.Entry<AID, Double>> list, AID key) {
        for (java.util.Map.Entry<AID, Double> entry : list) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return -1;
    }
    public double calculateUtility(double offerPrice) {
        // Weights: Tune as needed
        double urgencyWeight = 0.5;
        double priceWeight = 0.3;
        double batteryWeight = 0.2;

        // Normalized price impact (lower price = better utility)
        double priceImpact = 1.0 - (offerPrice / (meanPrice + 1e-5));
        double utility = (urgencyWeight * chargingUrgency)
                + (priceWeight * priceImpact)
                + (batteryWeight * (1 - batteryRatio));
        return utility;
    }

    private double generateInitialBid() {
        double baseWillingness = chargingUrgency * meanPrice;
        // Make sure bid is affordable
        double bid = Math.min(money, baseWillingness * 0.8);
        return roundToCents(bid);
    }

    private double generateNextBid(double lastBid, double sellerCounter) {
        negotiationRound++;

        double maxWillingToPay = Math.min(
                money,
                chargingUrgency * 1.5 * meanPrice
        );

        // Increase bid slightly based on how urgent the need is and how far off the counter is
        double increaseFactor = 0.1 * negotiationRound + (sellerCounter - lastBid) * 0.1;

        double nextBid = lastBid + increaseFactor * meanPrice;

        if (nextBid > maxWillingToPay || lastBid == -1) {
            return -1; // Signal that the buyer refuses to go higher
        }

        return roundToCents(nextBid);
    }

    private double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
