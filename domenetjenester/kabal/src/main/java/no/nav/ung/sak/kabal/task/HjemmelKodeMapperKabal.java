package no.nav.ung.sak.kabal.task;

import no.nav.ung.kodeverk.hjemmel.Hjemmel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HjemmelKodeMapperKabal {

    private static final Map<Hjemmel, String> HJEMMEL_TO_KABAL_MAP = Map.ofEntries(
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_1, "FS_UNG_1"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_2, "FS_UNG_2"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_3, "FS_UNG_3"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_4, "FS_UNG_4"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_6, "FS_UNG_6"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_7, "FS_UNG_7"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_8, "FS_UNG_8"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_9, "FS_UNG_9"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_10, "FS_UNG_10"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_11, "FS_UNG_11"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_12, "FS_UNG_12"),
        Map.entry(Hjemmel.UNG_FORSKRIFT_PARAGRAF_14, "FS_UNG_14"),
        Map.entry(Hjemmel.ARBEIDSMARKEDSLOVEN_PARAGRAF_12, "ARBML_12"),
        Map.entry(Hjemmel.ARBEIDSMARKEDSLOVEN_PARAGRAF_13, "ARBML_13"),
        Map.entry(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_11, "FVL_11"),
        Map.entry(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_28, "FVL_28"),
        Map.entry(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_29, "FVL_29"),
        Map.entry(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_30, "FVL_30"),
        Map.entry(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_31, "FVL_31"),
        Map.entry(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_34, "FVL_34"),
        Map.entry(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_35, "FVL_35"),
        Map.entry(Hjemmel.FOLKETRYGDLOVEN_PARAGRAF_22_15, "FTRL_22_15"),
        Map.entry(Hjemmel.FOLKETRYGDLOVEN_PARAGRAF_22_17, "FTRL_22_17"),
        Map.entry(Hjemmel.FOLKETRYGDLOVEN_PARAGRAF_22_17_A, "FTRL_22_17A")
    );

    public static List<String> mapHjemlerToKabalCodes(List<Hjemmel> localHjemler) {
        return localHjemler.stream()
            .map(HJEMMEL_TO_KABAL_MAP::get)
            .filter(Objects::nonNull)
            .toList();
    }
}
