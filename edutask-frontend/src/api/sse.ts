type StreamSseOptions<T> = {
  signal?: AbortSignal;
  onMessage: (message: T) => void;
};

export async function readSseStream<T>(
  response: Response,
  { onMessage }: StreamSseOptions<T>,
): Promise<void> {
  const reader = response.body?.getReader();

  if (!reader) {
    throw new Error('SSE поток недоступен');
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();

    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    buffer = consumeSseBuffer(buffer, onMessage);
  }

  buffer += decoder.decode();
  consumeSseBuffer(`${buffer}\n\n`, onMessage);
}

function consumeSseBuffer<T>(buffer: string, onMessage: (message: T) => void): string {
  const normalizedBuffer = buffer.replace(/\r\n/g, '\n');
  const events = normalizedBuffer.split('\n\n');
  const rest = events.pop() ?? '';

  events.forEach((eventText) => {
    const data = eventText
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n');

    if (!data) {
      return;
    }

    onMessage(JSON.parse(data) as T);
  });

  return rest;
}
