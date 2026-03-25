package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
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
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class ForutgåendeMedlemskapsvilkårSteg implements BehandlingSteg {

    private final SøknadParser søknadParser;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final MottatteDokumentRepository mottatteDokumentRepository;


    @Inject
    public ForutgåendeMedlemskapsvilkårSteg(VilkårResultatRepository vilkårResultatRepository,
                                            MottatteDokumentRepository mottatteDokumentRepository,
                                            SøknadParser søknadParser) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var vilkårOpt = vilkårene.getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        var harAvklartMedlemskap = !vilkårOpt.map(vilkår -> vilkår.getPerioder().stream().anyMatch(periode -> Utfall.IKKE_VURDERT.equals(periode.getUtfall()))).orElse(true);

        if (harAvklartMedlemskap) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Søknad nyesteSøknad = finnNyesteSøknad(kontekst);

        if (nyesteSøknad == null) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP));
        }

        Bosteder forutgåendeBosteder = ((Aktivitetspenger) nyesteSøknad.getYtelse()).getForutgåendeBosteder();
        var aksjonspunkter = vurderForutgåendeMedlemskap(forutgåendeBosteder);

        if (aksjonspunkter.isEmpty()) {
            var medlemskapsvilkår = vilkårOpt.orElseThrow(
                () -> new IllegalStateException("Forutgående medlemsskapsvilkåret ikke initialisert"));

            oppfyllVilkår(vilkårene, medlemskapsvilkår, kontekst.getBehandlingId());

            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private Søknad finnNyesteSøknad(BehandlingskontrollKontekst kontekst) {
        return mottatteDokumentRepository.hentMottatteDokumentForBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId(), List.of(Brevkode.AKTIVITETSPENGER_SOKNAD), false, DokumentStatus.GYLDIG)
            .stream()
            .sorted(Comparator.comparing(MottattDokument::getMottattTidspunkt).reversed())
            .collect(Collectors.toCollection(LinkedHashSet::new)).stream().findFirst()
            .map(søknadParser::parseSøknad)
            .orElse(null);
    }

    private void oppfyllVilkår(Vilkårene vilkårene, Vilkår tidligereVilkår, Long behandlingId) {
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        tidligereVilkår.getPerioder().stream()
            .map(it -> vilkårBuilder.hentBuilderFor(it.getPeriode()).medUtfall(Utfall.OPPFYLT))
            .forEach(vilkårBuilder::leggTil);

        vilkårResultatBuilder.leggTil(vilkårBuilder);

        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());

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
        return List.of(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }
}
