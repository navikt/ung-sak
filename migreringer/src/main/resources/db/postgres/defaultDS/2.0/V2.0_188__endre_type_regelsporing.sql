alter table VR_VILKAR_PERIODE alter column regel_evaluering TYPE oid using (regel_evaluering::oid);
alter table VR_VILKAR_PERIODE alter column regel_input TYPE oid using (regel_input::oid);
