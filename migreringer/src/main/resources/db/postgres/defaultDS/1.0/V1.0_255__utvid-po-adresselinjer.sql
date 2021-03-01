-- utvider littegrann for utenlandske adresser - PDL har ingen validering, kan være opp til 4000 tegn teknisk, som er riv ruskende galt
ALTER TABLE po_adresse
    ALTER COLUMN adresselinje1 TYPE varchar(75),
    ALTER COLUMN adresselinje2 TYPE varchar(75),
    ALTER COLUMN adresselinje3 TYPE varchar(75)
    ;
    
update po_adresse
  set adresselinje1=trim(adresselinje1)
    , adresselinje2=trim(adresselinje2)
    , adresselinje3=trim(adresselinje3)
  where (adresselinje1 like ' %' or adresselinje1 like '% ')
    or (adresselinje2 like ' %' or adresselinje2 like '% ')
    or (adresselinje3 like ' %' or adresselinje3 like '% ')
;