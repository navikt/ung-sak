package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnskontrollAktivitetDto;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;

@ApplicationScoped
public class TotrinnskontrollAktivitetDtoTjeneste {
    private OpptjeningsperioderTjeneste opptjeningsperioderTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    protected TotrinnskontrollAktivitetDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TotrinnskontrollAktivitetDtoTjeneste(OpptjeningsperioderTjeneste opptjeningsperioderTjeneste,
                                                VilkårResultatRepository vilkårResultatRepository,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                VirksomhetTjeneste virksomhetTjeneste,
                                                InntektArbeidYtelseTjeneste iayTjeneste,
                                                ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public List<TotrinnskontrollAktivitetDto> hentAktiviterEndretForOpptjening(Totrinnsvurdering aksjonspunkt,
                                                                               Behandling behandling,
                                                                               Optional<UUID> iayGrunnlagUuid) {
        if (AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING.equals(aksjonspunkt.getAksjonspunktDefinisjon())) {
            Long behandlingId = behandling.getId();
            List<OpptjeningsperiodeForSaksbehandling> aktivitetPerioder = new ArrayList<>();
            LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
            BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
            var vilkår = vilkårResultatRepository.hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(VilkårType.OPPTJENINGSVILKÅRET));
            if (vilkår.isPresent()) {
                var iayGrunnlag = iayTjeneste.hentGrunnlag(iayGrunnlagUuid.get());
                var opptjeningsresultat = opptjeningsperioderTjeneste.hentOpptjeningResultat(behandlingId);

                for (var opptjeningperiode : vilkår.get().getPerioder()) {
                    LocalDate stp = opptjeningperiode.getFom();
                    var opptjening = opptjeningsresultat.finnOpptjening(stp).orElseThrow(() -> new IllegalStateException("Mangler opptjening for stp:" + stp));
                    aktivitetPerioder.addAll(opptjeningsperioderTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingReferanse, iayGrunnlag, opptjening, stp));
                }
            }
            return aktivitetPerioder.stream()
                .filter(periode -> periode.erManueltBehandlet() || periode.getBegrunnelse() != null)
                .map(this::lagDtoAvPeriode)
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private TotrinnskontrollAktivitetDto lagDtoAvPeriode(OpptjeningsperiodeForSaksbehandling periode) {
        TotrinnskontrollAktivitetDto dto = new TotrinnskontrollAktivitetDto();
        dto.setAktivitetType(periode.getOpptjeningAktivitetType());
        dto.setErEndring(periode.getErPeriodeEndret());
        dto.setGodkjent(erPeriodeGodkjent(periode));

        Arbeidsgiver arbeidsgiver = periode.getArbeidsgiver();
        if (arbeidsgiver != null) {
            mapArbeidsgiverOpplysninger(dto, arbeidsgiver);
        }
        return dto;
    }

    private void mapArbeidsgiverOpplysninger(TotrinnskontrollAktivitetDto dto, Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.erAktørId()) {
            ArbeidsgiverOpplysninger arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            if (arbeidsgiverOpplysninger != null) {
                dto.setPrivatpersonFødselsdato(arbeidsgiverOpplysninger.getFødselsdato());
                dto.setArbeidsgiverNavn(arbeidsgiverOpplysninger.getNavn());
            }
        } else if (arbeidsgiver.getOrgnr() != null) {
            String arbeidsgiverNavn = hentVirksomhetNavnPåOrgnr(arbeidsgiver.getOrgnr());
            dto.setArbeidsgiverNavn(arbeidsgiverNavn);
            dto.setOrgnr(new OrgNummer(arbeidsgiver.getOrgnr()));
        }
    }

    private boolean erPeriodeGodkjent(OpptjeningsperiodeForSaksbehandling periode) {
        return VurderingsStatus.GODKJENT.equals(periode.getVurderingsStatus())
            || VurderingsStatus.FERDIG_VURDERT_GODKJENT.equals(periode.getVurderingsStatus());
    }

    private String hentVirksomhetNavnPåOrgnr(String orgnr) {
        if (orgnr == null) {
            return null;
        }
        Optional<Virksomhet> virksomhet = virksomhetTjeneste.finnOrganisasjon(orgnr);
        if (!virksomhet.isPresent()) {
            virksomhet = virksomhetTjeneste.finnOrganisasjon(orgnr);
        }
        return virksomhet.map(Virksomhet::getNavn)
            .orElseThrow(() -> new IllegalStateException("Finner ikke virksomhet med orgnr " + orgnr));
    }

}
