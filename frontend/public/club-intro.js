/* Programming Club: a 4.3-second opening identity. No dependencies. */
(() => {
  const SHAPE = 'M125 86 L111 103 C101 90 91 86 80 87 C65 87 54 99 54 116 C54 132 65 144 79 144 C90 144 98 137 108 125 L143 82 C153 70 166 65 179 65 C208 65 230 87 230 116 C230 145 208 168 179 168 C169 168 160 165 152 161 L152 187 L130 207 L130 144 L143 129 C154 141 164 147 177 147 C194 147 208 134 208 117 C208 100 195 87 179 87 C167 87 160 93 152 103 L116 147 C106 160 93 166 79 166 C52 166 32 145 32 116 C32 87 53 65 81 65 C99 65 113 72 125 86 Z';
  // Fixed vector wordmark: Nimbus Sans Bold, consistent spacing on every device.
  const WORDS = {"Programming": "M6.6 20.639 L14.829 20.639 C20.728 20.639 24.509 16.459 24.509 9.948 C24.509 3.52 20.859 0 14.169 0 L0 0 L0 32.079 L6.6 32.079 L6.6 20.639 Z M6.6 15.139 L6.6 5.5 L12.76 5.5 C16.28 5.5 17.909 7.04 17.909 10.299 C17.909 13.599 16.28 15.139 12.76 15.139 L6.6 15.139 Z M28.777 8.319 L28.777 32.079 L34.937 32.079 L34.937 19.449 C34.937 15.84 36.745 14.039 40.348 14.039 C41.008 14.039 41.448 14.08 42.287 14.217 L42.287 7.968 C41.936 7.92 41.805 7.92 41.537 7.92 C38.725 7.92 36.305 9.769 34.937 12.98 L34.937 8.319 L28.777 8.319 Z M56.364 7.92 C49.104 7.92 44.663 12.719 44.663 20.508 C44.663 28.339 49.104 33.089 56.412 33.089 C63.672 33.089 68.161 28.339 68.161 20.68 C68.161 12.629 63.803 7.92 56.364 7.92 Z M56.412 12.898 C59.801 12.898 62.001 15.929 62.001 20.598 C62.001 25.039 59.712 28.119 56.412 28.119 C53.064 28.119 50.823 25.08 50.823 20.508 C50.823 15.929 53.064 12.898 56.412 12.898 Z M87.607 8.319 L87.607 11.969 C85.888 9.199 83.997 7.92 81.488 7.92 C79.377 7.92 77.047 8.979 75.417 10.697 C73.045 13.159 71.808 16.589 71.808 20.769 C71.808 28.078 75.685 33.089 81.357 33.089 C83.908 33.089 85.448 32.258 87.607 29.659 L87.607 32.869 C87.607 35.729 85.585 37.709 82.677 37.709 C80.477 37.709 79.116 36.74 78.628 34.939 L72.296 34.939 C72.337 36.788 72.997 38.06 74.627 39.38 C76.517 40.92 79.116 41.669 82.457 41.669 C89.325 41.669 93.457 38.369 93.457 32.869 L93.457 8.319 L87.607 8.319 Z M82.636 13.069 C85.537 13.069 87.696 16.28 87.696 20.68 C87.696 24.997 85.627 27.94 82.547 27.94 C79.865 27.94 77.968 24.997 77.968 20.68 C77.968 16.197 79.865 13.069 82.636 13.069 Z M99.661 8.319 L99.661 32.079 L105.821 32.079 L105.821 19.449 C105.821 15.84 107.629 14.039 111.232 14.039 C111.892 14.039 112.332 14.08 113.171 14.217 L113.171 7.968 C112.82 7.92 112.689 7.92 112.421 7.92 C109.609 7.92 107.189 9.769 105.821 12.98 L105.821 8.319 L99.661 8.319 Z M137.065 31.329 C136.007 30.319 135.656 29.617 135.656 28.428 L135.656 15.228 C135.656 10.388 132.356 7.92 125.928 7.92 C119.507 7.92 116.165 10.649 115.767 16.149 L121.707 16.149 C122.016 13.688 123.027 12.898 126.065 12.898 C128.437 12.898 129.627 13.688 129.627 15.269 C129.627 16.06 129.228 16.72 128.568 17.119 C127.736 17.559 127.736 17.559 124.697 18.04 L122.236 18.48 C117.527 19.277 115.237 21.697 115.237 25.96 C115.237 30.229 118.097 33.089 122.456 33.089 C125.096 33.089 127.468 31.989 129.668 29.7 C129.668 30.938 129.805 31.377 130.376 32.079 L137.065 32.079 L137.065 31.329 Z M129.627 22.529 C129.627 26.098 127.867 28.119 124.745 28.119 C122.676 28.119 121.397 27.019 121.397 25.259 C121.397 23.409 122.367 22.529 124.917 22 L127.028 21.608 C128.657 21.299 128.925 21.209 129.627 20.859 L129.627 22.529 Z M141.111 8.319 L141.111 32.079 L147.271 32.079 L147.271 17.82 C147.271 14.877 148.852 13.159 151.492 13.159 C153.561 13.159 154.84 14.348 154.84 16.239 L154.84 32.079 L161 32.079 L161 17.82 C161 14.919 162.581 13.159 165.221 13.159 C167.291 13.159 168.569 14.348 168.569 16.239 L168.569 32.079 L174.729 32.079 L174.729 15.269 C174.729 10.649 171.911 7.92 167.16 7.92 C164.121 7.92 162.052 8.979 160.209 11.44 C159.061 9.199 156.689 7.92 153.74 7.92 C151.011 7.92 149.251 8.8 147.229 11.268 L147.229 8.319 L141.111 8.319 Z M180.227 8.319 L180.227 32.079 L186.387 32.079 L186.387 17.82 C186.387 14.877 187.968 13.159 190.608 13.159 C192.677 13.159 193.956 14.348 193.956 16.239 L193.956 32.079 L200.116 32.079 L200.116 17.82 C200.116 14.919 201.697 13.159 204.337 13.159 C206.407 13.159 207.685 14.348 207.685 16.239 L207.685 32.079 L213.845 32.079 L213.845 15.269 C213.845 10.649 211.027 7.92 206.276 7.92 C203.237 7.92 201.168 8.979 199.325 11.44 C198.177 9.199 195.805 7.92 192.856 7.92 C190.127 7.92 188.367 8.8 186.345 11.268 L186.345 8.319 L180.227 8.319 Z M225.812 8.319 L219.652 8.319 L219.652 32.079 L225.812 32.079 L225.812 8.319 Z M225.812 0 L219.652 0 L219.652 5.5 L225.812 5.5 L225.812 0 Z M231.705 8.319 L231.705 32.079 L237.865 32.079 L237.865 17.82 C237.865 15.008 239.804 13.159 242.836 13.159 C245.476 13.159 246.796 14.609 246.796 17.428 L246.796 32.079 L252.956 32.079 L252.956 16.149 C252.956 10.869 250.096 7.92 244.995 7.92 C241.784 7.92 239.625 9.068 237.865 11.749 L237.865 8.319 L231.705 8.319 Z M273.419 8.319 L273.419 11.969 C271.7 9.199 269.809 7.92 267.3 7.92 C265.189 7.92 262.859 8.979 261.229 10.697 C258.857 13.159 257.62 16.589 257.62 20.769 C257.62 28.078 261.497 33.089 267.169 33.089 C269.72 33.089 271.26 32.258 273.419 29.659 L273.419 32.869 C273.419 35.729 271.397 37.709 268.489 37.709 C266.289 37.709 264.928 36.74 264.44 34.939 L258.108 34.939 C258.149 36.788 258.809 38.06 260.439 39.38 C262.329 40.92 264.928 41.669 268.269 41.669 C275.137 41.669 279.269 38.369 279.269 32.869 L279.269 8.319 L273.419 8.319 Z M268.448 13.069 C271.349 13.069 273.508 16.28 273.508 20.68 C273.508 24.997 271.439 27.94 268.359 27.94 C265.677 27.94 263.78 24.997 263.78 20.68 C263.78 16.197 265.677 13.069 268.448 13.069 Z", "Club": "M28.071 11.392 C27.851 8.621 27.28 6.861 25.912 5.06 C23.451 1.801 19.491 0 14.692 0 C5.631 0 0 6.462 0 16.851 C0 27.191 5.583 33.612 14.52 33.612 C22.523 33.612 27.803 28.992 28.201 21.642 L21.78 21.642 C21.34 25.74 18.783 28.071 14.692 28.071 C9.591 28.071 6.6 23.932 6.6 16.94 C6.6 9.852 9.721 5.631 14.912 5.631 C18.741 5.631 20.9 7.48 21.78 11.392 L28.071 11.392 Z M38.939 0.523 L32.779 0.523 L32.779 32.601 L38.939 32.601 L38.939 0.523 Z M65.862 32.601 L65.862 8.841 L59.702 8.841 L59.702 23.712 C59.702 26.531 57.771 28.38 54.732 28.38 C52.092 28.38 50.772 26.971 50.772 24.111 L50.772 8.841 L44.612 8.841 L44.612 25.383 C44.612 30.663 47.472 33.612 52.58 33.612 C55.791 33.612 57.942 32.471 59.702 29.782 L59.702 32.601 L65.862 32.601 Z M71.544 0.523 L71.544 32.601 L77.704 32.601 L77.704 30.181 C79.196 32.512 81.265 33.612 84.256 33.612 C89.935 33.612 94.245 28.201 94.245 21.031 C94.245 17.82 93.276 14.561 91.695 12.272 C90.065 9.941 87.164 8.442 84.256 8.442 C81.265 8.442 79.196 9.543 77.704 11.921 L77.704 0.523 L71.544 0.523 Z M82.895 13.592 C85.926 13.592 88.085 16.672 88.085 21.072 C88.085 25.431 85.975 28.462 82.895 28.462 C79.766 28.462 77.704 25.52 77.704 20.941 C77.704 16.582 79.815 13.592 82.895 13.592 Z"};
  class ProgrammingClubIntro extends HTMLElement {
    connectedCallback() {
      if(this.shadowRoot?.childNodes.length)return;
      const root=this.shadowRoot||this.attachShadow({mode:'open'});
      this.abort=new AbortController();this.reduced=matchMedia('(prefers-reduced-motion: reduce)');
      this.timer=null;this.exitTimer=null;this.finished=true;this.target=null;
      root.innerHTML=`
      <style>
        :host{display:block;width:100%;color:#edf3ff;font-family:system-ui,-apple-system,sans-serif;color-scheme:dark;container-type:inline-size}
        :host([hidden]){display:none!important}:host([overlay]){position:fixed;inset:0;z-index:10000;height:100dvh}
        *{box-sizing:border-box}.scene{--lock-width:var(--intro-lock,480px);--mark-width:var(--intro-mark,144px);--intro-x:calc((var(--intro-lock,480px) - var(--intro-mark,144px)) / 2);--intro-y:0px;position:relative;overflow:hidden;background:var(--intro-bg,#080f1c);height:var(--intro-height,420px);isolation:isolate}:host([overlay]) .scene{height:100%}
        .scene::before{content:'';position:absolute;inset:0;background:radial-gradient(ellipse at center,var(--intro-glow,#14284245),transparent 65%);pointer-events:none}
        .identity{position:absolute;left:50%;top:50%;width:var(--lock-width);transform:translate(-50%,-50%);display:grid;grid-template-columns:var(--mark-width) minmax(0,1fr);gap:32px;align-items:center}
        .mark{width:var(--mark-width);transform:translate(var(--intro-x),var(--intro-y)) scale(1.22);transform-origin:center;will-change:transform}.mark svg,.type svg{display:block;width:100%;height:auto;overflow:visible}
        .guide{fill:none;stroke:#50749b;stroke-width:.5;opacity:0}.trace{fill:none;stroke:#b2d5ff;stroke-width:1;stroke-linejoin:round;stroke-linecap:round;stroke-dasharray:1;stroke-dashoffset:1;opacity:0}.brand{fill:url(#pc-color);opacity:0}
        .type{width:100%;min-width:0}.word{transform:translateY(48px);opacity:0}.suffix{display:inline-block;margin-top:clamp(8px,1cqw,16px);padding:.38em .7em;border:1px solid var(--intro-suffix-border,#2b415c);border-radius:6px;font-family:var(--intro-suffix-font,ui-monospace,SFMono-Regular,Consolas,monospace);font-size:clamp(11px,1.5cqw,19px);line-height:1;letter-spacing:.04em;color:#b4cded;transform:translateY(48px);opacity:0}.first{fill:#edf3ff}.second{fill:#b4cded}
        .skip,.replay{position:absolute;background:transparent;border:0;padding:12px;color:#93a8c2;font:12px/1.4 system-ui,-apple-system,sans-serif;min-height:44px;cursor:pointer}.skip{top:16px;right:16px}.replay{bottom:16px;right:16px;display:none}.skip:hover,.replay:hover{color:#f0f6ff}.skip:focus-visible,.replay:focus-visible{outline:2px solid #b2d5ff;outline-offset:2px;border-radius:4px}
        :host([mark-only]) .type{display:none}:host([mark-only]) .playing .mark{animation:none}:host([mark-only]) .done .mark{transform:translate(var(--intro-x),var(--intro-y)) scale(1.22)}
        .sr{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
        .playing .guide{animation:guide 1.8s ease both}.playing .trace{animation:trace 1.3s cubic-bezier(.35,0,.3,1) .1s both,trace-out .45s ease 1.4s forwards}.playing .brand{animation:fill .5s ease 1.35s forwards}.playing .mark{animation:dock .7s cubic-bezier(.22,1,.36,1) 1.95s forwards}.playing .first{animation:word .6s cubic-bezier(.22,1,.36,1) 2.35s forwards}.playing .second{animation:word .6s cubic-bezier(.22,1,.36,1) 2.48s forwards}.playing .suffix{animation:word .6s cubic-bezier(.22,1,.36,1) 2.62s forwards}
        .done .mark{transform:translate(0,0) scale(1)}.done .brand{opacity:1}.done .trace,.done .guide{opacity:0}.done .word{transform:translateY(0);opacity:1}.done .suffix{transform:translateY(0);opacity:1}.done .skip{display:none}:host([preview]) .done .replay{display:block}.leaving{animation:exit .35s ease forwards;pointer-events:none}
        @keyframes trace{0%{stroke-dashoffset:1;opacity:1}100%{stroke-dashoffset:0;opacity:1}}@keyframes trace-out{to{opacity:0}}@keyframes guide{0%{opacity:0}15%,65%{opacity:.65}100%{opacity:0}}@keyframes fill{to{opacity:1}}@keyframes dock{to{transform:translate(0,0) scale(1)}}@keyframes word{to{transform:translateY(0);opacity:1}}@keyframes exit{to{opacity:0;transform:translateY(-10px)}}
        @container(max-width:560px){.scene{height:440px;--lock-width:min(280px,calc(100cqw - 48px));--mark-width:136px;--intro-x:0px;--intro-y:57px}.identity{grid-template-columns:minmax(0,1fr);gap:26px;justify-items:center}.type{max-width:280px}.second-position{transform:translate(92.88px,54px)}}
        @media(prefers-reduced-motion:reduce){*,*::before{animation:none!important;transition:none!important}}
      </style>
      <section class="scene" aria-label="Programming Club introduction">
        <div class="identity">
          <div class="mark" aria-hidden="true"><svg viewBox="30 63 202 146">
            <defs><linearGradient id="pc-color" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#9cc7fc"/><stop offset=".52" stop-color="#6798df"/><stop offset="1" stop-color="#3c6baa"/></linearGradient></defs>
            <path class="guide" d="${SHAPE}"/>
            <path class="brand" d="${SHAPE}"/>
            <path class="trace" pathLength="1" d="${SHAPE}"/>
          </svg></div>
          <div class="type" aria-hidden="true"><svg viewBox="0 0 280 90">
            <defs><clipPath id="pc-first"><rect x="-2" y="-2" width="284" height="48"/></clipPath><clipPath id="pc-second"><rect x="-2" y="-2" width="284" height="40"/></clipPath></defs>
            <g clip-path="url(#pc-first)"><path class="word first" d="${WORDS.Programming}"/></g>
            <g class="second-position" transform="translate(0 54)"><g clip-path="url(#pc-second)"><path class="word second" d="${WORDS.Club}"/></g></g>
          </svg><span class="suffix">@ DAU</span></div>
        </div>
        <span class="sr">Programming Club</span>
        <button type="button" class="replay">Replay intro</button>
      </section>`;

      this.scene=root.querySelector('.scene');
      root.querySelector('.skip')?.addEventListener('click',()=>this.finish('skipped'),{signal:this.abort.signal});
      root.querySelector('.replay').addEventListener('click',()=>this.play(),{signal:this.abort.signal});
      document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!this.finished&&this.hasAttribute('overlay'))this.finish('skipped');},{signal:this.abort.signal});
      this.reduced.addEventListener('change',e=>{if(e.matches&&!this.finished)this.finish('reduced-motion');},{signal:this.abort.signal});
      let seen=false;
      if(this.hasAttribute('once')){try{seen=sessionStorage.getItem(this.storageKey)==='1';}catch{}}
      if(seen){this.finished=false;this.finish('already-seen');}else this.play();
    }
    get storageKey(){return this.getAttribute('storage-key')||'programming-club:intro:v2';}
    play(){
      clearTimeout(this.timer);clearTimeout(this.exitTimer);this.finished=false;this.hidden=false;
      this.scene.className='scene';
      if(this.hasAttribute('overlay')&&!this.target){
        const targetId=this.getAttribute('for');this.target=targetId?document.getElementById(targetId):null;
        if(this.target){this.oldInert=this.target.inert;this.target.inert=true;}
        this.oldFocus=document.activeElement;
        this.shadowRoot.querySelector('.skip')?.focus({preventScroll:true});
      }
      if(this.reduced.matches){this.finish('reduced-motion');return;}
      void this.scene.offsetWidth;this.scene.classList.add('playing');
      this.timer=setTimeout(()=>this.finish('complete'),this.hasAttribute('mark-only')?2200:4300);
    }
    finish(reason='skipped'){
      if(this.finished)return;
      this.finished=true;clearTimeout(this.timer);clearTimeout(this.exitTimer);
      this.scene.className='scene done';
      const complete=()=>{
        if(this.hasAttribute('overlay'))this.hidden=true;
        this.release();
        if(this.hasAttribute('once')){try{sessionStorage.setItem(this.storageKey,'1');}catch{}}
        this.dispatchEvent(new CustomEvent('intro-complete',{bubbles:true,composed:true,detail:{reason}}));
      };
      if(this.hasAttribute('overlay')&&reason==='complete'&&!this.reduced.matches){this.scene.classList.add('leaving');this.exitTimer=setTimeout(complete,350);}else complete();
    }
    release(){
      if(this.target){this.target.inert=this.oldInert;this.target=null;}
      if(this.hasAttribute('overlay')&&this.oldFocus?.isConnected&&this.oldFocus!==document.body)this.oldFocus.focus({preventScroll:true});
      this.oldFocus=null;
    }
    disconnectedCallback(){clearTimeout(this.timer);clearTimeout(this.exitTimer);this.abort?.abort();this.release();if(this.shadowRoot)this.shadowRoot.innerHTML='';}
  }
  if(!customElements.get('programming-club-intro'))customElements.define('programming-club-intro',ProgrammingClubIntro);
})();
