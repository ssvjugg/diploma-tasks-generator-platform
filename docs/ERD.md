```dbml
Table user_profile {
  id uuid [pk, default: `gen_random_uuid()`]
  keycloak_id varchar(255) [not null, unique]
  role varchar(10) [not null, note: 'STUDENT, TEACHER, ADMIN']
  created_at timestamptz [not null, default: `now()`]
  updated_at timestamptz [not null, default: `now()`]
}

Table task {
  id uuid [pk, default: `gen_random_uuid()`]
  title varchar(255) [not null]
  statement text [not null]
  input_format text
  output_format text
  difficulty varchar(10) [not null, note: 'EASY, MEDIUM, HARD']
  author_id uuid [not null]
  created_at timestamptz [not null, default: `now()`]
  updated_at timestamptz [not null, default: `now()`]
}

Table test_case {
  id uuid [pk, default: `gen_random_uuid()`]
  task_id uuid [not null]
  input_data text [not null]
  expected_output text [not null]
  is_hidden boolean [not null, default: false]
  points integer [not null, default: 0, note: '>= 0']
  created_at timestamptz [not null, default: `now()`]
  updated_at timestamptz [not null, default: `now()`]
}

Table programming_language {
  id integer [pk, increment]
  name varchar(100) [not null]
  code varchar(50) [not null, unique]
  judge0_language_id integer [not null]
}

Table topic {
  id uuid [pk, default: `gen_random_uuid()`]
  name varchar(255) [not null]
  parent_id uuid
}

Table task_topic {
  task_id uuid [not null]
  topic_id uuid [not null]
}

Table submission {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null]
  task_id uuid [not null]
  language_id integer [not null]
  source_code text [not null]
  status varchar(30) [not null, note: 'QUEUED, PROCESSING, ACCEPTED, WRONG_ANSWER, COMPILATION_ERROR, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, FAILED']
  score integer [not null, default: 0, note: '>= 0']
  max_score integer [not null, default: 0, note: '>= 0']
  passed_tests integer [not null, default: 0]
  total_tests integer [not null, default: 0]
  error_message text
  created_at timestamptz [not null, default: `now()`]
  updated_at timestamptz [not null, default: `now()`]
}

Table submission_test_result {
  id uuid [pk, default: `gen_random_uuid()`]
  submission_id uuid [not null]
  test_case_id uuid
  test_case_index integer [not null]
  is_hidden boolean [not null, default: false]
  input_data text
  expected_output text
  points integer [not null, default: 0, note: '>= 0']
  status varchar(30) [not null, note: 'QUEUED, PROCESSING, ACCEPTED, WRONG_ANSWER, COMPILATION_ERROR, RUNTIME_ERROR, TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED, FAILED']
  actual_output text
  stderr text
  compile_output text
  error_message text
  judge_token varchar(255)
  time numeric(10,3)
  memory integer
  created_at timestamptz [not null, default: `now()`]
}

Table user_model_settings {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null]
  provider_type varchar(50) [not null]
  base_url varchar(500)
  model_name varchar(255) [not null]
  encrypted_api_key text [not null]
  temperature numeric(3,2) [note: '0..2']
  is_default boolean [not null, default: false]
  is_active boolean [not null, default: true]
  created_at timestamptz [not null, default: `now()`]
  updated_at timestamptz [not null, default: `now()`]
}

Table chat_session {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null]
  settings_id uuid
  task_id uuid
  submission_id uuid
  title varchar(255)
  model_provider varchar(50) [not null]
  model_name varchar(255) [not null]
  created_at timestamptz [not null, default: `now()`]
  updated_at timestamptz [not null, default: `now()`]
  last_message_at timestamptz
}

Table chat_message {
  id uuid [pk, default: `gen_random_uuid()`]
  session_id uuid [not null]
  role varchar(10) [not null, note: 'USER, ASSISTANT, SYSTEM']
  content text [not null]
  message_order integer [not null, note: '>= 0']
  status varchar(10) [not null, note: 'CREATED, SENT, COMPLETED, FAILED']
  model_provider varchar(50)
  model_name varchar(255)
  prompt_tokens integer [note: '>= 0']
  completion_tokens integer [note: '>= 0']
  total_tokens integer [note: '>= 0']
  error_message text
  created_at timestamptz [not null, default: `now()`]
}

Table generation_request {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null]
  user_prompt text
  final_prompt text
  status varchar(20) [not null, note: 'QUEUED, PROCESSING, COMPLETED, FAILED']
  model_provider varchar(50)
  model_name varchar(255)
  generated_task_id uuid [unique]
  generated_draft jsonb
  error_message text
  created_at timestamptz [not null, default: `now()`]
  updated_at timestamptz [not null, default: `now()`]
}

Ref: task.author_id > user_profile.id

Ref: test_case.task_id > task.id [delete: cascade]

Ref: topic.parent_id > topic.id [delete: set null]

Ref: task_topic.task_id > task.id [delete: cascade]
Ref: task_topic.topic_id > topic.id [delete: cascade]

Ref: submission.user_id > user_profile.id
Ref: submission.task_id > task.id
Ref: submission.language_id > programming_language.id

Ref: submission_test_result.submission_id > submission.id [delete: cascade]
Ref: submission_test_result.test_case_id > test_case.id [delete: set null]

Ref: user_model_settings.user_id > user_profile.id [delete: cascade]

Ref: chat_session.user_id > user_profile.id [delete: cascade]
Ref: chat_session.settings_id > user_model_settings.id [delete: set null]
Ref: chat_session.task_id > task.id [delete: set null]
Ref: chat_session.submission_id > submission.id [delete: set null]

Ref: chat_message.session_id > chat_session.id [delete: cascade]

Ref: generation_request.user_id > user_profile.id
Ref: generation_request.generated_task_id > task.id [delete: set null]
```