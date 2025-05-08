package org.agents.exceptions;

public class InvalidServiceSpecification extends RuntimeException {

    public InvalidServiceSpecification(final Throwable cause) {
        super("Couldn't create agent's service.", cause);
    }
}
