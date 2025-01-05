"use strict";

import {
  html,
  render,
  useState,
  useEffect,
  useRef,
} from "https://unpkg.com/htm@3.1.1/preact/standalone.module.js";

import hljs from "https://unpkg.com/@highlightjs/cdn-assets@11.9.0/es/highlight.min.js";
import java from "https://unpkg.com/@highlightjs/cdn-assets@11.9.0/es/languages/java.min.js";
import { jarNames } from "./dist/jar_files.js";

hljs.registerLanguage("java", java);

const getFromStorageOr = (key, defaultValue, fun) => {
  const saved = localStorage.getItem(key);
  if (saved === null) {
    return defaultValue;
  } else {
    if (fun == null) {
      return saved;
    } else {
      return fun(saved);
    }
  }
};

const initialInput = getFromStorageOr("input", "class A {}");
const initialFileName = getFromStorageOr("file_name", "A");

const App = () => {
  const [fileName, setFileName] = useState(initialFileName);
  const [output, setOutput] = useState("");
  const [input, setInput] = useState(initialInput);
  const mainRef = useRef(null);
  const running = useRef(false);
  const cm = useRef(null);

  useEffect(() => {
    if (cm.current === null) {
      cm.current = CodeMirror(document.getElementById("input"), {
        lineNumbers: true,
        matchBrackets: true,
        value: input,
        mode: "text/x-java",
      });
      cm.current.setSize("100%", "100%");
    }
    return () => {};
  }, [input]);

  useEffect(() => {
    (async () => {
      if (mainRef.current === null) {
        setOutput("loading ...");
        await cheerpjInit();
        mainRef.current = await cheerpjRunLibrary(
          jarNames.map((x) => "/app/javap-web/dist/" + x).join(":"),
        );
      }
      if (running.current === true) {
        console.log("skip");
      } else {
        running.current = true;
        try {
          const encoder = new TextEncoder();
          const baseDir = (Math.random() + 1).toString(36).substring(2);
          const javaFile = `/str/${baseDir}/${fileName}.java`;
          cheerpOSAddStringFile(javaFile, encoder.encode(input));
          const main = await mainRef.current.javap_web.Main;
          const classOutput = `/files/${baseDir}/`;
          await main.mkdir(classOutput);
          const javacResult = await main.javac([
            javaFile,
            "-d",
            classOutput,
            "-Xlint",
          ]);
          if (javacResult != null && javacResult != "") {
            setOutput(javacResult);
          } else {
            const javapResult = await main.javap(classOutput);
            const result = await javapResult.result();
            const err = await javapResult.error();
            if (result != null) {
              setOutput(result);
            } else {
              setOutput(err);
            }
          }
        } finally {
          running.current = false;
        }
      }
    })();
  }, [input, fileName]);

  async function formatInput() {
    const main = await mainRef.current.javap_web.Main;
    const code = await main.format(input);
    setInput(code);
    cm.current.setValue(code);
  }

  [
    ["input", input],
    ["file_name", fileName],
  ].forEach(([key, val]) => {
    if (val.toString().length <= 1024 * 8) {
      localStorage.setItem(key, val);
    }
  });

  return html` <div class="row">
    <div class="col">
      <div class="row">
        <div class="col">
          <label for="file_name">file name: </label>
          <input
            maxlength="128"
            id="file_name"
            value=${fileName}
            oninput=${(x) => setFileName(x.target.value)}
          /><span>.java</span>
        </div>
        <div class="col">
          <button class="btn btn-secondary" onclick=${() => formatInput()}>
            format input java code
          </button>
        </div>
      </div>
      <div
        id="input"
        style="width: 100%; height: 800px"
        onkeyup=${(e) => setInput(cm.current.getValue())}
        onChange=${(e) => setInput(cm.current.getValue())}
      ></div>
    </div>
    <div class="col">
      <pre
        style="width: 100%; height: 800px; background-color:rgb(66, 66, 66);"
      >
${output}</pre
      >
    </div>
  </div>`;
};

render(html`<${App} />`, document.getElementById("root"));
