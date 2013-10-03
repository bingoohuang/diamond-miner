package org.n3r.diamond.client;

public class DiamondException extends RuntimeException{
    public DiamondException() {
    }

    public DiamondException(Throwable cause) {
        super(cause);
    }

    public static class Missing extends DiamondException {

    }

    public static class WrongType extends DiamondException {

        public WrongType(Throwable cause) {
            super(cause);
        }
    }
}
