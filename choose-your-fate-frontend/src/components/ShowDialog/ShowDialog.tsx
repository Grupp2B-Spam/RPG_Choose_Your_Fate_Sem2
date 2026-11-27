import { useEffect, useState } from "react";
import { useTypewriter } from "../../hooks/useTypewriter";
import type { Choice, Scene } from "../../types/general";
import "./ShowDialog.css";

type Props = {
  nextscene: Scene;
  changeScene: (destinationSceneId: string | number, choiceId: string | number) => void;
};

export function ShowDialog({ nextscene, changeScene }: Props) {
  const [index, setIndex] = useState(0);
  const [lastDialog, setLastDialog] = useState(false);
  const text = nextscene.dialog[index];
  const typed = useTypewriter(text);

  useEffect(() => {
    setIndex(0);
    setLastDialog(false);
  }, [nextscene.id]);

  function nextLine() {
    if (index < nextscene.dialog.length - 1) {
      setIndex(index + 1);
    }
    if(index >= nextscene.dialog.length - 2){
      setLastDialog(true);
    }
  }

  return (
    <div className="App" onClick={nextLine}>
      <img
        src={nextscene.img}
        alt="sceneId"
        style={{ width: "100vw", height: "99vh", objectFit: "contain" }}
      />

      <div className="dialogContainer">
        <div className="dialogTextContainer">
          {typed}
        </div>
      </div>
      {lastDialog && (
      <div className="dialogChoiceContainer">
        {nextscene.choices.map((choice: Choice) => (
          <div
            key={choice.id}
            className="choice"
            onClick={(event) => {
              event.stopPropagation();
              changeScene(choice.destination_id, choice.id);
            }}
          >
            {choice.name}
          </div>
        ))}
      </div>
      )}
    </div>
  );
}
