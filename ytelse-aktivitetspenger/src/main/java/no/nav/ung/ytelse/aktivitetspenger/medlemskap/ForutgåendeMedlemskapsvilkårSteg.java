package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Aktivitetspenger;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Bosteder;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FORUTGÅENDE_MEDLEMSKAPVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class ForutgåendeMedlemskapsvilkårSteg implements BehandlingSteg {

    private final SøknadParser søknadParser;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;


    @Inject
    public ForutgåendeMedlemskapsvilkårSteg(BehandlingRepository behandlingRepository,
                                            VilkårResultatRepository vilkårResultatRepository,
                                            MottatteDokumentRepository mottatteDokumentRepository,
                                            SøknadParser søknadParser,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var medlemskapsvilkår = vilkårene.getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        var harAvklartMedlemskap = !medlemskapsvilkår.map(vilkår -> vilkår.getPerioder().stream().anyMatch(periode -> Utfall.IKKE_VURDERT.equals(periode.getUtfall()))).orElse(true);

        if (harAvklartMedlemskap) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Søknad nyesteSøknad = mottatteDokumentRepository.hentMottatteDokumentForBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId(), List.of(Brevkode.AKTIVITETSPENGER_SOKNAD), false, DokumentStatus.GYLDIG)
            .stream()
            .sorted(Comparator.comparing(MottattDokument::getMottattTidspunkt).reversed())
            .collect(Collectors.toCollection(LinkedHashSet::new)).stream().findFirst()
            .map(søknadParser::parseSøknad)
            .orElse(null);

        Bosteder forutgåendeBosteder = ((Aktivitetspenger) nyesteSøknad.getYtelse()).getForutgåendeBosteder();
        var aksjonspunkter = vurderForutgåendeMedlemskap(forutgåendeBosteder);

        if (aksjonspunkter.isEmpty()) {

            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }


    static List<AksjonspunktDefinisjon> vurderForutgåendeMedlemskap(Bosteder forutgåendeBosteder) {
        if (forutgåendeBosteder.getPerioder().isEmpty()) {
            return Collections.emptyList();
        }

        boolean alleLandGyldige = forutgåendeBosteder.getPerioder().entrySet().stream()
            .allMatch(entry -> TrygdeavtaleLandOppslag.erGyldigTrygdeavtaleLand(
                entry.getValue().getLand(),
                entry.getKey().getFraOgMed()
            ));

        if (alleLandGyldige) {
            return Collections.emptyList();
        }
        return List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
    }
}
