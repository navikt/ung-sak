alter table PB_PLEIEPERIODE
    add column pleiegrad varchar(40);

update PB_PLEIEPERIODE
set pleiegrad = case
                    when grad = 0 then 'KONTINUERLIG_TILSYN'
                    when grad = 1 then 'UTVIDET_KONTINUERLIG_TILSYN'
                    when grad = 2 then 'INNLEGGELSE'
                    when grad = 3 then 'INGEN'
                    when grad = 4 then 'INGEN'
                    when grad = 5 then 'UDEFINERT'
    end
where pleiegrad is null;

alter table PB_PLEIEPERIODE
    alter column grad DROP NOT NULL;
