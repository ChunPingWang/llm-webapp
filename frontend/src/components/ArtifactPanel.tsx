import { useState } from "react";
import { downloadArtifact, extractArtifacts, type Artifact } from "../lib/artifacts";
import { Markdown } from "./Markdown";

/** Artifact 面板:抽取 Gherkin / Java code fence,提供高亮、複製、下載(R2–R4)。 */
export function ArtifactPanel({
  sourceMarkdown,
  onExpand,
}: {
  sourceMarkdown: string;
  onExpand?: () => void;
}) {
  const artifacts = extractArtifacts(sourceMarkdown);

  if (artifacts.length === 0) {
    return <p className="empty">尚無產出物。當助理回覆包含 Gherkin 或 Java code fence 時,會在此列出。</p>;
  }

  return (
    <div className="artifacts">
      {onExpand && (
        <div className="word-toolbar">
          <span className="word-status">{artifacts.length} 個產出物</span>
          <button className="expand-btn" onClick={onExpand}>⤢ 放大</button>
        </div>
      )}
      {artifacts.map((a) => (
        <ArtifactCard key={a.index} artifact={a} />
      ))}
    </div>
  );
}

function ArtifactCard({ artifact }: { artifact: Artifact }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    await navigator.clipboard.writeText(artifact.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return (
    <div className="artifact-card">
      <div className="artifact-head">
        <span className={`badge badge-${artifact.type.toLowerCase()}`}>{artifact.type}</span>
        <span className="artifact-lang">.{artifact.ext}</span>
        <div className="artifact-actions">
          <button onClick={copy}>{copied ? "已複製" : "複製"}</button>
          <button onClick={() => downloadArtifact(artifact)}>下載</button>
        </div>
      </div>
      <Markdown>{"```" + artifact.lang + "\n" + artifact.content + "\n```"}</Markdown>
    </div>
  );
}
