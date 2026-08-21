package no.nav.ung.sak.formidling.vedtak.regler;

public record VedtaksbrevEgenskaper(
    boolean kanHindre,
    boolean kanOverstyreHindre,
    boolean kanRedigere,
    boolean kanOverstyreRediger
) {

    public static VedtaksbrevEgenskaper kanRedigere(boolean kanRedigere) {
        return new VedtaksbrevEgenskaper(kanRedigere, kanRedigere, kanRedigere, kanRedigere);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean kanHindre;
        private boolean kanOverstyreHindre;
        private boolean kanRedigere;
        private boolean kanOverstyreRediger;

        public Builder kanHindre(boolean kanHindre) {
            this.kanHindre = kanHindre;
            return this;
        }

        public Builder kanOverstyreHindre(boolean kanOverstyreHindre) {
            this.kanOverstyreHindre = kanOverstyreHindre;
            return this;
        }

        public Builder kanRedigere(boolean kanRedigere) {
            this.kanRedigere = kanRedigere;
            return this;
        }

        public Builder kanOverstyreRediger(boolean kanOverstyreRediger) {
            this.kanOverstyreRediger = kanOverstyreRediger;
            return this;
        }

        public VedtaksbrevEgenskaper build() {
            return new VedtaksbrevEgenskaper(kanHindre, kanOverstyreHindre, kanRedigere, kanOverstyreRediger);
        }
    }
}
