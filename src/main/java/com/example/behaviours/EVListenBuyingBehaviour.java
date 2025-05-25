package com.example.behaviours;

import com.example.domain.Map;
import jade.core.AID;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.example.agents.EVAgent;

import java.util.*;

public class EVListenBuyingBehaviour extends OneShotBehaviour {
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
        maxRounds = 2;//(int)(1 + chargingUrgency * 5);

        System.out.printf("[%s] Init values: meanPrice=%.2f, urgency=%.2f, money=%.2f\n",
                myAgent.getLocalName(), meanPrice, chargingUrgency, money);

        System.out.printf("[%s] starting negotiations\n", myAgent.getLocalName());
        beginNegotiations();
    }

    private void beginNegotiations() {

        List<AID> allEVs = evAgent.getEvInQueue();
        List<ACLMessage> replies = new ArrayList<>();

        List<java.util.Map.Entry<AID, Double>> oldBids = new ArrayList<>();
        List<java.util.Map.Entry<AID, Double>> newBids = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        long timeout = 2000;
        int count = allEVs.size();
        double initialBid = generateInitialBid();

        AID finalSeller = null;
        double finalPrice = Double.MAX_VALUE;

        // Send initial offer
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        msg.setContent(String.format(Locale.US, "%.2f", initialBid));
        for (AID ev : allEVs) {
            msg.addReceiver(ev);
        }
        evAgent.send(msg);

        // Main negotiation loop
        while (true) {

            long startOfRound = System.currentTimeMillis();
            System.out.printf("[%s] negotiations, round %d\n", myAgent.getLocalName(), negotiationRound);

            MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);

            // Wait for replies
            while (System.currentTimeMillis() - startOfRound < timeout) {
                ACLMessage reply = evAgent.receive(template);

                if (reply != null) {
                    replies.add(reply);
                    //System.out.printf("[%s]: received a message from %s\n", myAgent.getLocalName(), reply.getSender().getLocalName());
                } else {
                    block(100); // Pause a bit to avoid CPU overload
                }
            }

            // Finalizing negotiations
            if (negotiationRound >= maxRounds) {

                // Collect proposals and decide which to buy
                double counterBid;

                if (replies.isEmpty())
                    break;

                for (ACLMessage reply : replies) {
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {

                        try {
                            // TEMPORARY IF STATEMENT, LATER USE UTILITY
                            counterBid = Double.parseDouble(reply.getContent().trim());
                            double utility = calculateUtility(counterBid);
                            if (counterBid < finalPrice) {
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
                finalYes.addReceiver(finalSeller);
                evAgent.send(finalYes);
                break;
            }

            // Collect proposals and calculate next bids
            for (ACLMessage reply : replies) {
                if (reply.getPerformative() == ACLMessage.PROPOSE && reply.getConversationId().equals("propose_from_seller")) {
                    double counterBid;
                    AID sender = reply.getSender();

                    try {
                        counterBid = Double.parseDouble(reply.getContent().trim());
                        if (negotiationRound == 1) {
                            double newBid = generateNextBid(initialBid, counterBid);
                            System.out.printf("[%s] Received bid from %s: %.2f, calculated counter: %.2f\n",
                                    evAgent.getLocalName(), sender.getLocalName(), counterBid, newBid);
                            if (newBid != -1) {
                                newBids.add(new AbstractMap.SimpleEntry<>(
                                        sender,
                                        newBid
                                ));
                            }
                        } else {
                            double newBid = generateNextBid(getValueByKey(oldBids, sender), counterBid);
                            if (newBid != -1) {
                                newBids.add(new AbstractMap.SimpleEntry<>(
                                        sender,
                                        newBid
                                ));
                                System.out.printf("[%s] Received bid from %s: %.2f, calculated counter: %.2f\n",
                                        evAgent.getLocalName(), sender.getLocalName(), counterBid, newBid);
                            }
                        }


                    } catch (NumberFormatException e) {
                        System.out.println(evAgent.getLocalName()
                                + " ERROR received bad message during negotiation");
                    }
                }
            }

            // Send proposals and rejection messages

            ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            for (java.util.Map.Entry<AID, Double> entry : newBids) {
                if (entry.getValue() != -1) {
                    msg = new ACLMessage(ACLMessage.PROPOSE);
                    msg.setConversationId("propose_from_buyer");
                    msg.setContent(String.format(Locale.US, "%.2f", entry.getValue()));
                    msg.addReceiver(entry.getKey());
                    evAgent.send(msg);
                } else {
                    rejectMsg.addReceiver(entry.getKey());
                }
            }
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
                ACLMessage message = myAgent.blockingReceive(messageTemplate, 1000);

                if (message != null && message.getConversationId().equals("csInform")) {
                    String content = message.getContent();
                    String[] parts = content.split(":");
                    Map.Station station = Map.getStationByName(parts[0]);
                    evAgent.setCpId(parts[1]);

                    if (finalSeller != null) {
                        float time = (float) (System.currentTimeMillis() - startTime) / 1000;
                        System.out.printf("[%s]: finalized the negotiations with %s, it took %.3f seconds\n", evAgent.getLocalName(), finalSeller.getLocalName(), time);
                    }

                    evAgent.travelToCp(station);
                    evAgent.setEvInQueue(new ArrayList<>());

                    evAgent.addBehaviour(new EVResellingBehaviour(evAgent));
                    return;
                }
                tryCount++;
            }
        }

        System.out.printf("[%s]: receives no counter-bids or someone else got the switch message first\n", evAgent.getLocalName());

        // If all rejected then go back to asking CS's
        evAgent.setCurrentCommunication(evAgent.getCurrentLocation());
        evAgent.setCurrentCommunicationAid(new AID(evAgent.getCurrentCommunication().name(), AID.ISLOCALNAME));
        evAgent.setEvInQueue(new ArrayList<>());

        evAgent.setSlotToRequest(evAgent.getSlotToRequest() + 1);
        evAgent.sortStations(evAgent.getCurrentLocation());
        evAgent.addBehaviour(new EVRequestCharging(evAgent));

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
        return (urgencyWeight * chargingUrgency)
                + (priceWeight * priceImpact)
                + (batteryWeight * (1 - batteryRatio));
    }

    private double generateInitialBid() {
        double baseWillingness = (1 + chargingUrgency) * meanPrice;
        // Make sure bid is affordable
        return Math.min(money, baseWillingness * 0.6);
    }

    private double generateNextBid(double lastBid, double sellerCounter) {

        double maxWillingToPay = Math.min(
                money,
                (1 + chargingUrgency) * meanPrice
        );

        // Buffer to allow tolerance near the edge of willingness
        double toleranceBuffer = 0.05 * meanPrice; // e.g., 5% leeway

        // Use urgency to modulate aggression
        double urgencyFactor = Math.max(0.05, Math.min(0.3, chargingUrgency * 0.1));

        // Compute a gradual increase
        double increase = urgencyFactor * (sellerCounter - lastBid);
        increase = Math.max(increase, 0.02 * meanPrice); // Ensure a minimum step

        double nextBid = lastBid + increase;

        // Accept nextBid if it's within a buffer above max willingness
        if (nextBid <= maxWillingToPay + toleranceBuffer) {
            return nextBid;
        }

        return -1; // Still too much
    }
}
