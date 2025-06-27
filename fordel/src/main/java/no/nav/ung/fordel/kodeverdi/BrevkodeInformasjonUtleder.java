package no.nav.ung.fordel.kodeverdi;

import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.FordelBehandlingType;

import java.util.*;


public final class BrevkodeInformasjonUtleder {

    private static Map<String, BrevkodeInformasjon> brevkodeInformasjon = createTitler();

    private BrevkodeInformasjonUtleder() {
    }

    public static Optional<BrevkodeInformasjon> getBrevkodeInformasjon(String brevkode) {
        return Optional.ofNullable(brevkodeInformasjon.get(brevkode));
    }

    /**
     * titler for papirdokumenter.  Digitale søknader får tittel ved innsending.
     */
    private static Map<String, BrevkodeInformasjon> createTitler() {
        final List<BrevkodeInformasjon> brevkodeliste = new ArrayList<>();
        // Fra Brukerdialog:

        brevkodeliste.add(new BrevkodeInformasjon(Brevkode.UNGDOMSYTELSE_SOKNAD.getOffisiellKode(), null, FagsakYtelseType.UNGDOMSYTELSE, null, FordelBehandlingType.DIGITAL_SØKNAD));
        brevkodeliste.add(new BrevkodeInformasjon("UNG Endringssøknad", null, FagsakYtelseType.UNGDOMSYTELSE, null, FordelBehandlingType.DIGITAL_SØKNAD));
        brevkodeliste.add(new BrevkodeInformasjon(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING.getOffisiellKode(), null, FagsakYtelseType.UNGDOMSYTELSE, null, FordelBehandlingType.DIGITAL_SØKNAD));
        brevkodeliste.add(new BrevkodeInformasjon(Brevkode.UNGDOMSYTELSE_ETTERLYSNING_UTTALELSE.getOffisiellKode(), null, FagsakYtelseType.UNGDOMSYTELSE, null, FordelBehandlingType.DIGITAL_SØKNAD));

        final Map<String, BrevkodeInformasjon> titler = new HashMap<>();
        brevkodeliste.forEach(bi -> {
            titler.putIfAbsent(bi.getBrevkode(), bi);
            bi.getAlternativBrevkode().ifPresent(b -> titler.put(b, bi));
        });
        return titler;
    }

    public static class BrevkodeInformasjon {
        private final String brevkode;
        private final String alternativBrevkode;
        private final FagsakYtelseType ytelseType;
        private final BehandlingTema behandlingTema;
        private final FordelBehandlingType fordelBehandlingTypeHvisStrukturert;

        public BrevkodeInformasjon(String brevkode, String alternativBrevkode, FagsakYtelseType ytelseType,
                                   BehandlingTema behandlingTema, FordelBehandlingType fordelBehandlingTypeHvisStrukturert) {
            this.brevkode = brevkode;
            this.alternativBrevkode = alternativBrevkode;
            this.ytelseType = ytelseType;
            this.behandlingTema = behandlingTema;
            this.fordelBehandlingTypeHvisStrukturert = fordelBehandlingTypeHvisStrukturert;
        }

        public String getBrevkode() {
            return brevkode;
        }

        public Optional<String> getAlternativBrevkode() {
            return Optional.ofNullable(alternativBrevkode);
        }

        public Optional<FagsakYtelseType> getYtelseType() {
            return Optional.ofNullable(ytelseType);
        }

        public Optional<BehandlingTema> getBehandlingTema() {
            return Optional.ofNullable(behandlingTema);
        }

        public Optional<FordelBehandlingType> getBehandlingTypeHvisStrukturert() {
            return Optional.ofNullable(fordelBehandlingTypeHvisStrukturert);
        }
    }
}
