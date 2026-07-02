-- SaaS Phase 4: মোবাইল/ইমেইল দিয়ে লগইন — phone/mobile কলামে গ্লোবাল UNIQUE
-- আগে backup নিয়ে নিন: mysqldump car_rental_db > db/backups/pre_phase4_mobile_login_<date>.sql
-- বিদ্যমান ডেটায় duplicate phone/mobile আছে কিনা যাচাই করা হয়েছে — কোনোটাই নেই (নিরাপদ)

ALTER TABLE users ADD UNIQUE KEY uniq_users_phone (phone);
ALTER TABLE drivers ADD UNIQUE KEY uniq_drivers_mobile (mobile);
ALTER TABLE managers ADD UNIQUE KEY uniq_managers_mobile (mobile);
