drop table BEREGNINGSGRUNNLAG cascade;
drop table BEREGNINGSGRUNNLAG_PERIODE cascade;
drop table BG_AKTIVITET cascade;
drop table BG_AKTIVITETER cascade;
drop table BG_AKTIVITET_OVERSTYRING cascade;
drop table BG_AKTIVITET_OVERSTYRINGER cascade;
drop table BG_AKTIVITET_STATUS cascade;
drop table BG_ANDEL_ARBEIDSFORHOLD cascade;
drop table BG_ARBEIDSTAKER_ANDEL cascade;
drop table BG_FAKTA_BER_TILFELLE cascade;
drop table BG_FRILANS_ANDEL cascade;
drop table BG_PERIODE_AARSAK cascade;
drop table BG_PERIODE_REGEL_SPORING cascade;
drop table BG_PR_STATUS_OG_ANDEL cascade;
drop table BG_REFUSJON_OVERSTYRING cascade;
drop table BG_REFUSJON_OVERSTYRINGER cascade;
drop table BG_REGEL_SPORING cascade;
drop table BG_SG_PR_STATUS cascade;
drop table GR_BEREGNINGSGRUNNLAG cascade;
drop table MEDLEMSKAP_VILKAR_PERIODE cascade;
drop table MEDLEMSKAP_VILKAR_PERIODER cascade;
drop table SAMMENLIGNINGSGRUNNLAG cascade;

alter table BEHANDLING_VEDTAK_VARSEL 	drop column BEREGNING_RESULTAT_ID, 
										drop column ENDRET_DEKNINGSGRAD, 
										drop column ENDRET_STOENADSKONTO;
										
alter table HISTORIKKINNSLAG drop column BRUKER_KJOENN;

alter table TOTRINNRESULTATGRUNNLAG drop column BEREGNINGSGRUNNLAG_ID, 
									drop column INNTEKT_ARBEID_GRUNNLAG_ID, 
									drop column UTTAK_RESULTAT_ID, 
									drop column YTELSES_FORDELING_GRUNNLAG_ID;

alter table UTTAK_RESULTAT_PERIODE 	drop column FLERBARNSDAGER, 
									drop column GRADERING_INNVILGET, 
									drop column MANUELT_BEHANDLET, 
									drop column OPPHOLD_AARSAK, 
									drop column OVERFOERING_AARSAK, 
									drop column SAMTIDIG_UTTAK, 
									drop column SAMTIDIG_UTTAKSPROSENT;
									
alter table VR_VILKAR_RESULTAT 	drop column OVERSTYRT,
								drop column VILKAR_RESULTAT;