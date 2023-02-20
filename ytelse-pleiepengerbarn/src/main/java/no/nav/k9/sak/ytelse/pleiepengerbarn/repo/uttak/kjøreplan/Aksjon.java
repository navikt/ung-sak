package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

public enum Aksjon {
    BEHANDLE, // Perioden kan behandles for fagsaken
    UTSETT, // Perioden skal utsettes
    VENTE_PÅ_ANNEN // Vente på andre fagsaker
}
