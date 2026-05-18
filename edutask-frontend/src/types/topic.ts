export type TopicSummary = {
  id: string;
  name: string;
};

export type Topic = TopicSummary & {
  parentId: string | null;
};
