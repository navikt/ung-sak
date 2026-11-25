alter table vilkaar_periode_avklaring
    alter column begrunnelse drop not null;

alter table vilkaar_periode_avklaring_foreslaatt
    alter column begrunnelse drop not null;
