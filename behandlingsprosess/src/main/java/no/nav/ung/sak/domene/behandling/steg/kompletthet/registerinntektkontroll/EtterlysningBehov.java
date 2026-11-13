package no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll;

public enum EtterlysningBehov {

    ERSTATT_EKSISTERENDE, // ERSTATT_EKSISTERENDE betyr at vi oppretter en etterlysning med ny frist uavhengig av om det finnes eksisterende etterlysning for samme periode
    BEHOLD_EKSISTERENDE, // BEHOLD_EKSISTERENDE betyr at vi ikke endrer på etterlysningene i perioden
    NY_ETTERLYSNING_DERSOM_INGEN_FINNES, // NY_ETTERLYSNING betyr at vi oppretter etterlysning kun dersom det ikke eksisterer en etterlysning for samme periode
    INGEN_ETTERLYSNING // INGEN_ETTERLYSNING betyr at vi ikke trenger etterlysning
    }
