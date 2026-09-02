<script setup lang="ts">
import AppLogo from "@/components/common/AppLogo.vue";
</script>

<template>
  <main class="auth-layout">
    <section class="auth-story">
      <AppLogo light />
      <div class="story-copy">
        <h1>让研究推进，<br><em>有迹可循。</em></h1>
        <p class="story-note">项目、文档、任务与 AI，<br>在一个工作空间中协同。</p>
      </div>
    </section>
    <section class="auth-content"><slot /></section>
  </main>
</template>

<style scoped>
.auth-layout { min-height: 100vh; display: grid; grid-template-columns: minmax(520px, 1.08fr) minmax(480px, .92fr); background: #f6f8fb; }
.auth-story { position: relative; overflow: hidden; padding: 48px 64px; display: flex; flex-direction: column; color: white; background: radial-gradient(circle at 88% 16%, rgba(40,100,195,.28), transparent 28%), linear-gradient(145deg, #091a39, #102d5c 60%, #16436e); }
.auth-story::before { content: ""; position: absolute; width: 560px; height: 560px; right: -210px; bottom: -230px; border: 1px solid rgba(105,222,205,.18); border-radius: 50%; box-shadow: 0 0 0 74px rgba(105,222,205,.025), 0 0 0 148px rgba(105,222,205,.018); }
.auth-story::after { content: ""; position: absolute; top: 18%; right: 13%; width: 98px; height: 98px; border-radius: 30px 8px 30px 30px; background: linear-gradient(145deg, rgba(101,212,194,.2), rgba(37,89,214,.05)); transform: rotate(17deg); }
.story-copy { position: relative; z-index: 1; max-width: 650px; margin: auto 0; }
.eyebrow { margin: 0 0 20px; color: #72dac9; font-size: 11px; font-weight: 800; letter-spacing: .22em; }
h1 { margin: 0; font: 600 clamp(46px, 5vw, 72px)/1.18 Georgia, "Noto Serif SC", serif; letter-spacing: -.05em; } h1 em { color: #eef5ff; font-style: normal; }
.story-note { max-width: 520px; margin: 30px 0 44px; color: #b3c1d6; font-size: 15px; line-height: 1.9; }
.story-features { display: flex; gap: 42px; }.story-features span { display: grid; gap: 7px; color: #edf4ff; font-size: 12px; }.story-features i { color: #6bd4c3; font: 500 10px/1 monospace; }
footer { position: relative; z-index: 1; display: flex; align-items: center; gap: 9px; color: #768aa9; font-size: 10px; letter-spacing: .12em; }.live-dot { width: 6px; height: 6px; border-radius: 50%; background: #62cfbd; box-shadow: 0 0 0 5px rgba(98,207,189,.08); }
.auth-content { display: grid; place-items: center; padding: 44px; }
@media (max-width: 900px) { .auth-layout { display: block; padding: 20px; background: linear-gradient(145deg, #091a39, #17436f); }.auth-story { display: none; }.auth-content { min-height: calc(100vh - 40px); padding: 18px; border-radius: 28px; background: #f8fafc; } }
</style>

<style scoped>
.auth-layout{grid-template-columns:minmax(520px,56fr) minmax(420px,44fr);background:radial-gradient(circle at 32% 43%,rgba(0,113,227,.075),transparent 34%),#f5f5f7}
.auth-story{padding:46px clamp(56px,6vw,104px);overflow:visible;color:var(--rf-ink);background:transparent}
.auth-story::before,.auth-story::after{display:none}
.story-copy{max-width:620px;margin:auto 0}
h1{max-width:620px;margin:0;font:600 clamp(46px,4.2vw,64px)/1.08 -apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif;letter-spacing:-.04em;white-space:nowrap}
h1 em{color:var(--rf-ink);font-style:normal}
.story-note{margin:28px 0 0;color:#6e6e73;font-size:17px;line-height:1.6}
.auth-content{padding:48px clamp(48px,6vw,92px) 48px 32px;background:transparent}
@media(max-width:1200px){h1{font-size:48px}.story-note{font-size:16px}.auth-content{padding-right:48px}}
@media(max-width:900px){.auth-layout{display:block;padding:20px;background:radial-gradient(circle at 50% 15%,rgba(0,113,227,.08),transparent 35%),#f5f5f7}.auth-story{display:none}.auth-content{min-height:calc(100vh - 40px);padding:20px;background:transparent}}
</style>

<style scoped>
.auth-story {
  padding: 48px clamp(52px, 5.5vw, 88px);
  background:
    radial-gradient(circle at 92% 8%, rgba(0,113,227,.12), transparent 30rem),
    linear-gradient(145deg, #fff 0%, #f5f9ff 62%, #edf5ff 100%);
}
.auth-story::before,
.auth-story::after { display: none; }
.story-copy { max-width: 720px; }
.eyebrow { margin-bottom: 24px; }
h1 {
  max-width: 680px;
  font-size: clamp(44px, 4vw, 58px);
  line-height: 1.12;
  letter-spacing: -.05em;
  white-space: nowrap;
}
.story-note {
  max-width: 610px;
  margin: 30px 0 0;
  color: #6e6e73;
  font-size: 16px;
  line-height: 1.8;
}
.auth-story footer { margin-top: auto; }
@media(max-width:1200px){h1{font-size:46px}.story-note{font-size:15px}}
</style>

<style scoped>
.auth-layout { grid-template-columns: minmax(520px,1.08fr) minmax(460px,.92fr); background: #f5f5f7; }
.auth-story { padding: 48px clamp(48px,6vw,92px); color: var(--rf-ink); background: radial-gradient(circle at 80% 18%,rgba(0,113,227,.15),transparent 28%),linear-gradient(145deg,#ffffff,#eef5ff 72%,#e1f0ff); }
.auth-story::before { border-color:rgba(0,113,227,.12); box-shadow:0 0 0 74px rgba(0,113,227,.025),0 0 0 148px rgba(0,113,227,.018); }
.auth-story::after { background:linear-gradient(145deg,rgba(0,113,227,.15),rgba(255,255,255,.2)); }
.eyebrow { color:var(--rf-blue); font-size:12px; letter-spacing:.14em; }
h1 { font:700 clamp(48px,5.2vw,78px)/1.08 -apple-system,BlinkMacSystemFont,"PingFang SC",sans-serif; letter-spacing:-.055em; }
h1 em { color:var(--rf-blue); }
.story-note { max-width:560px; color:#6e6e73; font-size:17px; line-height:1.75; }
.story-features { gap:48px; }.story-features span { color:#3d3d42; font-size:13px; }.story-features i { color:var(--rf-blue); }
footer { color:#86868b; font-size:11px; }.live-dot { background:#34c759; box-shadow:0 0 0 5px rgba(52,199,89,.1); }
.auth-content { padding:54px; background:rgba(245,245,247,.72); }
@media(max-width:900px){.auth-layout{background:linear-gradient(145deg,#eef5ff,#f5f5f7)}.auth-content{background:rgba(255,255,255,.72)}}
</style>
