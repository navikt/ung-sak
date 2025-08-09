alter table tilkjent_ytelse_periode
    add column avvik_avrunding numeric not null;

comment on column tilkjent_ytelse_periode.avvik_avrunding is 'Avvik grunnet avrunding til dagsats i hele kroner for perioden.';
