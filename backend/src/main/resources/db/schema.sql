DROP TABLE IF EXISTS job_run_log;
DROP TABLE IF EXISTS knowledge_qa;
DROP TABLE IF EXISTS material_template;
DROP TABLE IF EXISTS email_send_log;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS approval_rule;
DROP TABLE IF EXISTS payment_request;
DROP TABLE IF EXISTS invoice;
DROP TABLE IF EXISTS return_order_item;
DROP TABLE IF EXISTS return_order;
DROP TABLE IF EXISTS contract_info;
DROP TABLE IF EXISTS supplier_score_record;
DROP TABLE IF EXISTS budget_subject;
DROP TABLE IF EXISTS inbound_order_item;
DROP TABLE IF EXISTS inbound_order;
DROP TABLE IF EXISTS purchase_order_item;
DROP TABLE IF EXISTS purchase_order;
DROP TABLE IF EXISTS inquiry_quote;
DROP TABLE IF EXISTS inquiry;
DROP TABLE IF EXISTS purchase_approval;
DROP TABLE IF EXISTS purchase_request_item;
DROP TABLE IF EXISTS purchase_request;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS knowledge_chunk;
DROP TABLE IF EXISTS knowledge_doc;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(128) NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  dept_name VARCHAR(64),
  phone VARCHAR(32),
  email VARCHAR(128),
  status INT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  data_scope VARCHAR(16) DEFAULT 'SELF'
);

CREATE TABLE sys_user_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL
);

CREATE TABLE sys_oper_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  module VARCHAR(64),
  action VARCHAR(64),
  username VARCHAR(64),
  ip VARCHAR(64),
  detail VARCHAR(2000),
  method VARCHAR(16),
  request_uri VARCHAR(255),
  cost_ms BIGINT,
  success INT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE material (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  spec VARCHAR(128),
  unit VARCHAR(32),
  category VARCHAR(64),
  safe_stock DECIMAL(12,2) DEFAULT 0,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE supplier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  contact VARCHAR(64),
  phone VARCHAR(32),
  category VARCHAR(64),
  rating INT DEFAULT 3,
  status INT DEFAULT 1,
  remark VARCHAR(255),
  admission_score DECIMAL(5,2) DEFAULT 0,
  blacklisted INT DEFAULT 0,
  blacklist_reason VARCHAR(500),
  last_review_date DATE,
  next_review_date DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE supplier_score_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id BIGINT NOT NULL,
  score DECIMAL(5,2) NOT NULL,
  dimension VARCHAR(64),
  remark VARCHAR(500),
  reviewer_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT NOT NULL UNIQUE,
  quantity DECIMAL(12,2) DEFAULT 0,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE budget_subject (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  dept_name VARCHAR(64),
  budget_year INT NOT NULL,
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  used_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  remark VARCHAR(255),
  status INT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_no VARCHAR(64) NOT NULL UNIQUE,
  title VARCHAR(200) NOT NULL,
  applicant_id BIGINT NOT NULL,
  dept_name VARCHAR(64),
  budget_amount DECIMAL(14,2) DEFAULT 0,
  budget_subject_id BIGINT,
  required_levels INT DEFAULT 2,
  status VARCHAR(32) NOT NULL,
  remark VARCHAR(500),
  ai_prompt VARCHAR(1000),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_request_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  material_id BIGINT,
  material_name VARCHAR(128) NOT NULL,
  spec VARCHAR(128),
  unit VARCHAR(32),
  quantity DECIMAL(12,2) NOT NULL,
  estimate_price DECIMAL(12,2) DEFAULT 0,
  amount DECIMAL(14,2) DEFAULT 0,
  remark VARCHAR(255)
);

CREATE TABLE purchase_approval (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  level VARCHAR(8) NOT NULL,
  approver_id BIGINT NOT NULL,
  result VARCHAR(16) NOT NULL,
  comment VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE approval_rule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  min_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  max_amount DECIMAL(14,2) NOT NULL,
  levels INT NOT NULL,
  enabled INT DEFAULT 1,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inquiry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL,
  current_round INT DEFAULT 1,
  selected_supplier_id BIGINT,
  ai_recommend_json LONGTEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inquiry_quote (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  inquiry_id BIGINT NOT NULL,
  round_no INT DEFAULT 1,
  supplier_id BIGINT NOT NULL,
  unit_price DECIMAL(12,2),
  total_price DECIMAL(14,2),
  delivery_days INT,
  remark VARCHAR(255),
  negotiate_note VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  request_id BIGINT NOT NULL,
  inquiry_id BIGINT,
  supplier_id BIGINT NOT NULL,
  total_amount DECIMAL(14,2) DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  expect_date DATE,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  material_name VARCHAR(128) NOT NULL,
  spec VARCHAR(128),
  unit VARCHAR(32),
  quantity DECIMAL(12,2) NOT NULL,
  received_qty DECIMAL(12,2) DEFAULT 0,
  unit_price DECIMAL(12,2) DEFAULT 0,
  amount DECIMAL(14,2) DEFAULT 0
);

CREATE TABLE inbound_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  inbound_no VARCHAR(64) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  warehouse_user_id BIGINT NOT NULL,
  diff_flag INT DEFAULT 0,
  diff_remark VARCHAR(500),
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inbound_order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  inbound_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL,
  material_name VARCHAR(128),
  quantity DECIMAL(12,2) NOT NULL
);

CREATE TABLE return_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  return_no VARCHAR(64) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  type VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL,
  reason VARCHAR(500),
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE return_order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  return_id BIGINT NOT NULL,
  order_item_id BIGINT NOT NULL,
  material_name VARCHAR(128),
  quantity DECIMAL(12,2) NOT NULL,
  remark VARCHAR(255)
);

CREATE TABLE contract_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  contract_no VARCHAR(64) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  supplier_id BIGINT,
  title VARCHAR(200) NOT NULL,
  amount DECIMAL(14,2) DEFAULT 0,
  start_date DATE,
  end_date DATE,
  status VARCHAR(32) NOT NULL,
  content_text LONGTEXT,
  risk_summary LONGTEXT,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_no VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  supplier_id BIGINT,
  amount DECIMAL(14,2) NOT NULL,
  tax_amount DECIMAL(14,2) DEFAULT 0,
  invoice_date DATE,
  status VARCHAR(32) NOT NULL,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_no VARCHAR(64) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  invoice_id BIGINT,
  amount DECIMAL(14,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  applicant_id BIGINT,
  remark VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content VARCHAR(2000),
  biz_type VARCHAR(64),
  biz_id BIGINT,
  read_flag INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE email_send_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  to_addr VARCHAR(128),
  subject VARCHAR(200),
  content VARCHAR(2000),
  status VARCHAR(32),
  related_biz VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE material_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  material_id BIGINT,
  material_name VARCHAR(128),
  spec VARCHAR(128),
  unit VARCHAR(32),
  default_qty DECIMAL(12,2) DEFAULT 1,
  default_price DECIMAL(12,2) DEFAULT 0,
  use_count INT DEFAULT 0,
  last_used_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge_doc (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  file_name VARCHAR(255),
  file_path VARCHAR(500),
  chunk_count INT DEFAULT 0,
  status VARCHAR(32) DEFAULT 'READY',
  uploader_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge_chunk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doc_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content LONGTEXT NOT NULL,
  token_hint INT DEFAULT 0
);

CREATE TABLE knowledge_qa (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  question VARCHAR(1000) NOT NULL,
  answer LONGTEXT,
  references_json LONGTEXT,
  highlights_json LONGTEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attachment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  size BIGINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE sys_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_key VARCHAR(128) NOT NULL UNIQUE,
  config_value VARCHAR(2000),
  remark VARCHAR(255),
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_run_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  job_code VARCHAR(64) NOT NULL,
  job_name VARCHAR(128),
  trigger_type VARCHAR(32),
  status VARCHAR(32) NOT NULL,
  summary VARCHAR(1000),
  detail VARCHAR(2000),
  operator_name VARCHAR(64),
  duration_ms INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
