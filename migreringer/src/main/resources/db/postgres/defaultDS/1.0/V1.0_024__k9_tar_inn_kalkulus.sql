alter table TOTRINNRESULTATGRUNNLAG
add column beregningsgrunnlag_grunnlag_uuid UUID;

comment on column TOTRINNRESULTATGRUNNLAG.beregningsgrunnlag_grunnlag_uuid is 'Unik UUID for beregningsgrunnlag til utvortes bruk. Representerer en immutable og unikt identifiserbar instans av dette aggregatet';

create  index IDX_TORINN_RES_GR_07 on TOTRINNRESULTATGRUNNLAG (beregningsgrunnlag_grunnlag_uuid);
