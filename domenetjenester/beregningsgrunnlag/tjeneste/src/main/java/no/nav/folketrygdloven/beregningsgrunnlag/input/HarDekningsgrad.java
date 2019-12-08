package no.nav.folketrygdloven.beregningsgrunnlag.input;

/**
 * Trait for å legge til dekningsgrad på ytelsesspesifikt grunnlag. Bruk på samme klasse som implementerer {@link YtelsespesifiktGrunnlag}.
 */
public interface HarDekningsgrad extends YtelsespesifiktGrunnlag {

    int getDekningsgrad();
}
