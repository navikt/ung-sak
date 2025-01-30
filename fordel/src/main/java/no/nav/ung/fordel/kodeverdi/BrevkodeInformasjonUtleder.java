package no.nav.ung.fordel.kodeverdi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.FordelBehandlingType;


public final class BrevkodeInformasjonUtleder {

    private static Map<String, BrevkodeInformasjon> brevkodeInformasjon = createTitler();

    private BrevkodeInformasjonUtleder() {}


    public static String finnTittel(String brevkode) {
        if (brevkode == null) {
            return "Uten tittel";
        }
        var bi = brevkodeInformasjon.get(brevkode);
        if (bi == null) {
            return "Mangler tittel";
        }
        return bi.getTittel();
    }

    public static Optional<BrevkodeInformasjon> getBrevkodeInformasjon(String brevkode) {
        return Optional.ofNullable(brevkodeInformasjon.get(brevkode));
    }

    /** titler for papirdokumenter.  Digitale søknader får tittel ved innsending. */
    private static Map<String, BrevkodeInformasjon> createTitler() {
        final List<BrevkodeInformasjon> brevkodeliste = new ArrayList<>();
        // Fra Brukerdialog:

        brevkodeliste.add(new BrevkodeInformasjon(Brevkode.UNGDOMSYTELSE_SOKNAD.getOffisiellKode(), null, "Søknad om ungdomsytelse - UNG Søknad", FagsakYtelseType.UNGDOMSYTELSE, null, FordelBehandlingType.DIGITAL_SØKNAD, false));
        brevkodeliste.add(new BrevkodeInformasjon("UNG Endringssøknad", null, "Endringssøknad for ungdomsytelsen - UNG Endringssøknad", FagsakYtelseType.UNGDOMSYTELSE, null, FordelBehandlingType.DIGITAL_SØKNAD, false));
        brevkodeliste.add(new BrevkodeInformasjon(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING.getOffisiellKode(), null, "Inntektsrapportering for ungdomsytelsen", FagsakYtelseType.UNGDOMSYTELSE, null, FordelBehandlingType.DIGITAL_SØKNAD, false));

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
        private final String tittel;
        private final FagsakYtelseType ytelseType;
        private final BehandlingTema behandlingTema;
        private final FordelBehandlingType fordelBehandlingTypeHvisStrukturert;
        private final boolean ettersendelse;

        public BrevkodeInformasjon(String brevkode, String alternativBrevkode, String tittel) {
            this(brevkode, alternativBrevkode, tittel, null, null, null, false);
        }

        public BrevkodeInformasjon(String brevkode, String alternativBrevkode, String tittel, boolean ettersendelse) {
            this(brevkode, alternativBrevkode, tittel, null, null, null, ettersendelse);
        }

        public BrevkodeInformasjon(String brevkode, String alternativBrevkode, String tittel, FagsakYtelseType ytelseType,
                                   BehandlingTema behandlingTema, FordelBehandlingType fordelBehandlingTypeHvisStrukturert, boolean ettersendelse) {
            this.brevkode = brevkode;
            this.alternativBrevkode = alternativBrevkode;
            this.tittel = tittel;
            this.ytelseType = ytelseType;
            this.behandlingTema = behandlingTema;
            this.fordelBehandlingTypeHvisStrukturert = fordelBehandlingTypeHvisStrukturert;
            this.ettersendelse = ettersendelse;
        }

        public String getBrevkode() {
            return brevkode;
        }

        public Optional<String> getAlternativBrevkode() {
            return Optional.ofNullable(alternativBrevkode);
        }

        public String getTittel() {
            return tittel;
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

        public boolean isEttersendelse() {
            return ettersendelse;
        }
    }
}
