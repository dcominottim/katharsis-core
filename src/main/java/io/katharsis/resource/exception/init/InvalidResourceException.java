package io.katharsis.resource.exception.init;

import io.katharsis.errorhandling.exception.KatharsisInitializationException;

public class InvalidResourceException extends KatharsisInitializationException {

    public InvalidResourceException(String message) {
        super(message);
    }
}
