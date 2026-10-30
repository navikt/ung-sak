alter table beregningsgrunnlag add column grunnbeloep numeric(19, 2);

comment on column beregningsgrunnlag.grunnbeloep is 'Grunnbeløp (G) på skjæringstidspunktet som er brukt i beregningen';
