type ChatbotMessageContentProps = {
  text: string;
  className?: string;
};

type Section =
  | { type: 'paragraph'; text: string }
  | { type: 'heading'; text: string }
  | { type: 'list'; items: string[] };

const HEADING_PREFIXES = ['📌', '✅', '👉', '💡', '🙌'];

function isHeading(line: string) {
  return HEADING_PREFIXES.some((prefix) => line.startsWith(prefix));
}

function buildSections(text: string): Section[] {
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  const sections: Section[] = [];
  let listItems: string[] = [];

  const flushList = () => {
    if (listItems.length > 0) {
      sections.push({ type: 'list', items: listItems });
      listItems = [];
    }
  };

  lines.forEach((line) => {
    if (line.startsWith('- ')) {
      listItems.push(line.slice(2).trim());
      return;
    }

    flushList();

    if (isHeading(line)) {
      sections.push({ type: 'heading', text: line });
      return;
    }

    sections.push({ type: 'paragraph', text: line });
  });

  flushList();
  return sections;
}

export default function ChatbotMessageContent({ text, className }: ChatbotMessageContentProps) {
  const sections = buildSections(text);

  return (
    <div className={className}>
      {sections.map((section, index) => {
        if (section.type === 'heading') {
          return (
            <p key={`heading-${index}`} className="chatbot-message-heading">
              {section.text}
            </p>
          );
        }

        if (section.type === 'list') {
          return (
            <ul key={`list-${index}`} className="chatbot-message-list">
              {section.items.map((item, itemIndex) => (
                <li key={`item-${index}-${itemIndex}`}>{item}</li>
              ))}
            </ul>
          );
        }

        return (
          <p key={`paragraph-${index}`} className="chatbot-message-paragraph">
            {section.text}
          </p>
        );
      })}
    </div>
  );
}
