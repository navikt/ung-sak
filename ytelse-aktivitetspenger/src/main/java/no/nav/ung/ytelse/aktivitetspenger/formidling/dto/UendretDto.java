package no.nav.ung.ytelse.aktivitetspenger.formidling.dto;

import no.nav.ung.sak.formidling.innhold.TemplateInnholdDto;
import no.nav.ung.sak.typer.Periode;

public record UendretDto(
    boolean erVarsletSomOpphør,
    Periode periode,
    String fritekstBrev
) implements TemplateInnholdDto { }

