ALTER TABLE booking
    ADD CONSTRAINT uc_20667066603ca44de2db31f9a UNIQUE (room_id, check_in_date);

ALTER TABLE booking
    ADD CONSTRAINT uc_4b9fec80fb058be9deaa1480f UNIQUE (room_id, check_out_date);