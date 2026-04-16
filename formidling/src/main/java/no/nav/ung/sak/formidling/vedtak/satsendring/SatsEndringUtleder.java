package no.nav.ung.sak.formidling.vedtak.satsendring;

import java.util.ArrayList;
import java.util.List;

public class SatsEndringUtleder {

    private final List<SatsEndringUtlederInput> inputs;

    public SatsEndringUtleder(List<SatsEndringUtlederInput> inputs) {
        this.inputs = inputs;
    }

    public List<SatsEndringHendelseDto> lagSatsEndringHendelser() {
        List<SatsEndringHendelseDto> resultat = new ArrayList<>();
        SatsEndringUtlederInput previous = null;
        for (SatsEndringUtlederInput current : inputs) {
            if (previous == null) {
                previous = current;
                continue;
            }
            SatsEndring satsEndring = bestemSatsendring(current, previous);
            if (satsEndring.overgangLavSats()) {
                throw new IllegalStateException("Kan ikke ha overgang fra høy til lav sats mellom %s og %s".formatted(previous.fom(), current.fom()));
            }
            long barnetilleggSats = satsEndring.dødsfallBarn() ? previous.barnetilleggSats() : current.barnetilleggSats();
            resultat.add(new SatsEndringHendelseDto(
                satsEndring.overgangTilHøySats(),
                satsEndring.fødselBarn(),
                satsEndring.dødsfallBarn(),
                current.fom(),
                current.dagsats(),
                barnetilleggSats,
                satsEndring.fikkFlereBarn()
            ));
            previous = current;
        }
        return resultat;
    }

    private static SatsEndring bestemSatsendring(SatsEndringUtlederInput current, SatsEndringUtlederInput previous) {
        int gjeldendeAntallBarn = current.antallBarn();
        int tidligereAntallBarn = previous.antallBarn();
        var fødselBarn = gjeldendeAntallBarn > tidligereAntallBarn;
        var dødsfallBarn = gjeldendeAntallBarn < tidligereAntallBarn;
        var fikkFlereBarn = gjeldendeAntallBarn > tidligereAntallBarn && gjeldendeAntallBarn - tidligereAntallBarn > 1;
        var overgangTilHøySats = current.høySats() && !previous.høySats();
        var overgangLavSats = !current.høySats() && previous.høySats();
        return new SatsEndring(fødselBarn, dødsfallBarn, fikkFlereBarn, overgangTilHøySats, overgangLavSats);
    }
}

