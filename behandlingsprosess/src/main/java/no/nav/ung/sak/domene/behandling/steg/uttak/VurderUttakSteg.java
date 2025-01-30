package no.nav.ung.sak.domene.behandling.steg.uttak;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_UTTAK;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;

import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@BehandlingStegRef(value = VURDER_UTTAK)
@BehandlingTypeRef
@ApplicationScoped
public class VurderUttakSteg implements BehandlingSteg {
    private static final Logger LOGGER = Logger.getLogger(VurderUttakSteg.class.getName());

    private VilkårTjeneste vilkårTjeneste;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private PersonopplysningRepository personopplysningRepository;

    @Inject
    public VurderUttakSteg(VilkårTjeneste vilkårTjeneste, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository, UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, PersonopplysningRepository personopplysningRepository) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.personopplysningRepository = personopplysningRepository;
    }

    public VurderUttakSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var samletVilkårResultatTidslinje = vilkårTjeneste.samletVilkårsresultat(behandlingId);
        var godkjentePerioder = samletVilkårResultatTidslinje
            .mapValue(it -> it.getSamletUtfall().equals(Utfall.OPPFYLT))
            .filterValue(Boolean.TRUE::equals);

        if (godkjentePerioder.isEmpty()) {
            LOGGER.info("Ingen godkjente perioder funnet, ingen uttaksperioder vil bli lagret.");
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var ungdomsprogramtidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId)
            // Fjerner deler av programperiode som er etter søkte perioder.
            .intersection(new LocalDateTimeline<>(TIDENES_BEGYNNELSE, godkjentePerioder.getMaxLocalDate(), true));

        var søkersDødsdato = personopplysningRepository.hentPersonopplysninger(behandlingId)
            .getGjeldendeVersjon()
            .getPersonopplysninger()
            .stream()
            .filter(it -> it.getAktørId().equals(kontekst.getAktørId()))
            .findFirst()
            .map(PersonopplysningEntitet::getDødsdato);

        // TODO: Map verdier for sats og rapporterte inntekter
        var ungdomsytelseUttakPerioder = VurderUttakTjeneste.vurderUttak(godkjentePerioder, ungdomsprogramtidslinje, søkersDødsdato, LocalDateTimeline.empty(), LocalDateTimeline.empty());
        ungdomsytelseUttakPerioder.ifPresent(it -> ungdomsytelseGrunnlagRepository.lagre(behandlingId, it));
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
