package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Map;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.SkalForlengeAktivitetstatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

record SkalForlengeAktivitetstatusUtledningPerioder(
    Map<SkalForlengeAktivitetstatus.ForlengetAktivitetstatusKravType, Set<LocalDateInterval>> kravForForlengelseTidslinjer,
    Map<SkalForlengeAktivitetstatus.IngenForlengetAktivitetstatusÅrsak, Set<LocalDateInterval>> årsakTilIngenForlengelseTidslinjer) {
}
