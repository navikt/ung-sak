package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy;
public enum PolicyDecision {
    PERMIT {
        @Override
        public PolicyDecision and(PolicyDecision other) {
            return other;
        }

        @Override
        public PolicyDecision or(PolicyDecision other) {
            return PERMIT;
        }

        @Override
        public PolicyDecision not() {
            return DENY;
        }
    },

    DENY {
        @Override
        public PolicyDecision and(PolicyDecision other) {
            return DENY;
        }

        @Override
        public PolicyDecision or(PolicyDecision other) {
            return other;
        }

        @Override
        public PolicyDecision not() {
            return NOT_APPLICABLE;
        }
    },

    NOT_APPLICABLE {
        @Override
        public PolicyDecision and(PolicyDecision other) {
            return other == PERMIT ? NOT_APPLICABLE : other;
        }

        @Override
        public PolicyDecision or(PolicyDecision other) {
            return other == DENY ? DENY : other;
        }

        @Override
        public PolicyDecision not() {
            return NOT_APPLICABLE;
        }
    };

    public abstract PolicyDecision and(PolicyDecision other);

    public abstract PolicyDecision or(PolicyDecision other);

    public abstract PolicyDecision not();
}
