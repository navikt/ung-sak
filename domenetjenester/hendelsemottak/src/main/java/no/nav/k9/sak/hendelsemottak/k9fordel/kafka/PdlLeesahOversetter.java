package no.nav.k9.sak.hendelsemottak.k9fordel.kafka;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import javax.enterprise.context.Dependent;

import no.nav.k9.kodeverk.hendelser.HendelseKilde;
import no.nav.k9.sak.kontrakt.hendelser.DødfødtBarnHendelse;
import no.nav.k9.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.k9.sak.kontrakt.hendelser.Endringstype;
import no.nav.k9.sak.kontrakt.hendelser.FødselHendelse;
import no.nav.k9.sak.kontrakt.hendelser.Hendelse;
import no.nav.k9.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.k9.sak.typer.AktørId;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import no.nav.person.pdl.leesah.foedsel.Foedsel;

@Dependent
public class PdlLeesahOversetter {

    public static final String FØDSEL = "FOEDSEL_V1";
    public static final String DØDSFALL = "DOEDSFALL_V1";
    public static final String DØDFØDSEL = "DOEDFOEDT_BARN_V1";

    private final static int FNR_LENGTH = 11;

    public PdlLeesahOversetter() {
        // CDI
    }

    Optional<Hendelse> oversettPersonhendelse(Personhendelse personhendelse) {
        HendelseInfo hendelseInfo = mapHendelseInfo(personhendelse);
        if (FØDSEL.contentEquals(personhendelse.getOpplysningstype())) {
            return Optional.of(oversettFødsel(personhendelse, hendelseInfo));
        }
        if (DØDSFALL.contentEquals(personhendelse.getOpplysningstype())) {
            return Optional.of(oversettDødsfall(personhendelse, hendelseInfo));
        }
        if (DØDFØDSEL.contentEquals(personhendelse.getOpplysningstype())) {
            return Optional.of(oversettDødfødsel(personhendelse, hendelseInfo));
        }
        return Optional.empty();
    }

    private HendelseInfo mapHendelseInfo(Personhendelse personhendelse) {
        var builder = new HendelseInfo.Builder()
            .medHendelseKilde(HendelseKilde.PDL)
            .medHendelseId(personhendelse.getHendelseId().toString());
        if (personhendelse.getOpprettet() != null) {
            builder.medOpprettet(LocalDateTime.ofInstant(personhendelse.getOpprettet(), ZoneOffset.systemDefault()));
        }
        if (personhendelse.getEndringstype() != null) {
            builder.medEndringstype(Endringstype.valueOf(personhendelse.getEndringstype().name()));
        }

        for (CharSequence ident : personhendelse.getPersonidenter()) {
            if (ident.toString().length() == FNR_LENGTH) {
                // Bruker kun aktørId, ikke fnr
                continue;
            }
            builder.leggTilAktør(new AktørId(ident.toString()));
        }
        return builder.build();
    }

    private FødselHendelse oversettFødsel(Personhendelse personhendelse, HendelseInfo hendelseInfo) {
        FødselHendelse.Builder builder = new FødselHendelse.Builder();
        builder.medHendelseInfo(hendelseInfo);

        Foedsel foedsel = personhendelse.getFoedsel();
        if (foedsel != null) {
            builder.medFødselsdato(foedsel.getFoedselsdato());
        }
        return builder.build();
    }

    private DødsfallHendelse oversettDødsfall(Personhendelse personhendelse, HendelseInfo hendelseInfo) {
        DødsfallHendelse.Builder builder = new DødsfallHendelse.Builder();
        builder.medHendelseInfo(hendelseInfo);

        Doedsfall doedsfall = personhendelse.getDoedsfall();
        if (doedsfall != null) {
            builder.medDødsdato(doedsfall.getDoedsdato());
        }
        return builder.build();
    }

    private DødfødtBarnHendelse oversettDødfødsel(Personhendelse personhendelse, HendelseInfo hendelseInfo) {
        DødfødtBarnHendelse.Builder builder = new DødfødtBarnHendelse.Builder();
        builder.medHendelseInfo(hendelseInfo);

        if (personhendelse.getDoedfoedtBarn() != null) {
            builder.medDødfødselsdato(personhendelse.getDoedfoedtBarn().getDato());
        }
        return builder.build();
    }

}
