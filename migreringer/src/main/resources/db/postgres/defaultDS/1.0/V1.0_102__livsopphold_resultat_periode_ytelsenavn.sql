alter table livsopphold_resultat_periode
    add column ytelse_navn text;

comment on column livsopphold_resultat_periode.ytelse_navn is 'Navnet på ytelsen slik det vises i vedtaksbrevet, f.eks. ved MOTTAR_ANNEN_YTELSE.';
