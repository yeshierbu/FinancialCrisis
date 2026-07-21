ALTER TABLE system_account MODIFY password_hash VARCHAR(100) NOT NULL;
UPDATE system_account SET password_hash='$2y$10$nQZdR5KD44XeWMKOkYOX4OkZZ7HWbjvSHRbaasa05BPNq0k3HZ8DS' WHERE username='user';
UPDATE system_account SET password_hash='$2y$10$I56nZbKWOA6MxPLsSYttk.aKurpjdnUuDZmuUPQPj3fUfhUTrmJ3m' WHERE username='admin';
UPDATE system_account SET password_hash='$2y$10$nekz0Jz4ycTmZjh5niTmmezDemAW2Wyv.Kr2rZCMGpwE0dJ/chP0m' WHERE username='reviewer';
