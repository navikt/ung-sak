package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.mottak.SøknadParser;

@FagsakYtelseTypeRef("OMP_KS")
@BehandlingTypeRef
@RequestScoped
public class KroniskSykVilkårsVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private PersoninfoAdapter personTjeneste;

    KroniskSykVilkårsVurderingTjeneste() {
        // for proxy
    }

    @Inject
    public KroniskSykVilkårsVurderingTjeneste(BehandlingRepository behandlingRepository,
                                              PersoninfoAdapter personTjeneste,
                                              MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.personTjeneste = personTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var periode = utledPeriode(behandling);
        return new TreeSet<>(Set.of(periode));
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utled(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var periode = utledPeriode(behandling);
        return Map.of(VilkårType.UTVIDETRETT, new TreeSet<>(Set.of(periode)));
    }

    private DatoIntervallEntitet utledPeriode(Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var søknadBrevkode = Brevkode.SØKNAD_OMS_UTVIDETRETT_KS;
        var dokumenter = mottatteDokumentRepository.hentMottatteDokumentForBehandling(fagsakId, behandling.getId(), søknadBrevkode, true);

        if (dokumenter.size() != 1) {
            throw new UnsupportedOperationException("Støtter p.t. kun ett dokument per behandling, fikk " + dokumenter.size() + " knyttet til behandling");
        }
        var dok = dokumenter.get(0);
        var søknad = new SøknadParser().parseSøknad(dok);
        var ytelse = søknad.getYtelse();

        var barn = ytelse.getPleietrengende();

        var innhentetBarn = personTjeneste.innhentSaksopplysningerForBarn(PersonIdent.fra(barn.getPersonIdent().getVerdi()))
            .orElseThrow(() -> new IllegalStateException("Fant ikke barn for søknad i journalpost=" + dok.getJournalpostId()));
        var fom = innhentetBarn.getFødselsdato();
        var tom = fom.plusYears(18); // fram til 18 år

        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return Integer.MAX_VALUE;
    }
}