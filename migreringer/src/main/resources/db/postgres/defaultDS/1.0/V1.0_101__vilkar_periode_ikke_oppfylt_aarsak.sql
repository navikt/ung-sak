alter table vr_vilkar_periode add column ikke_oppfylt_aarsak varchar(100);

comment on column vr_vilkar_periode.ikke_oppfylt_aarsak is 'Detaljert årsak til at vilkåret ikke er oppfylt, som kode fra vilkårets IkkeOppfyltDetaljertÅrsak. Null for vilkår uten detaljerte årsaker.';
