package no.nav.ung.sak.domene.behandling.steg.kompletthet;

public enum EtterlysningBehov {

    ERSTATT_EKSISTERENDE, // ERSTATT_EKSISTERENDE betyr at vi oppretter en etterlysning med ny frist uavhengig av om det finnes eksisterende etterlysning for samme periode
    NY_ETTERLYSNING_DERSOM_INGEN_FINNES, // NY_ETTERLYSNING betyr at vi oppretter etterlysning kun dersom det ikke eksisterer en etterlysning for samme periode
    INGEN_ETTERLYSNING // INGEN_ETTERLYSNING betyr at vi ikke trenger etterlysning

    }
