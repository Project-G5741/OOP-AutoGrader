import { useEffect, useRef } from 'react';
import * as THREE from 'three';
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js';

const THREE_RARITY_PALETTE = {
  crystal: {
    main: 0xf59e0b,
    emissive: 0xd97706,
    bright: 0xfef08a,
    deep: 0xb45309,
    ring: 0xfbbf24,
    light: 0xfffbeb,
  },
  legendary: {
    main: 0xf59e0b,
    emissive: 0xd97706,
    bright: 0xfde047,
    deep: 0xb45309,
    ring: 0xfbbf24,
    light: 0xfffbeb,
  },
  epic: {
    main: 0xa855f7,
    emissive: 0x7e22ce,
    bright: 0xe9d5ff,
    deep: 0x6b21a8,
    ring: 0xc084fc,
    light: 0xfaf5ff,
  },
  rare: {
    main: 0x10b981,
    emissive: 0x047857,
    bright: 0xa7f3d0,
    deep: 0x065f46,
    ring: 0x34d399,
    light: 0xecfdf5,
  },
  normal: {
    main: 0x38bdf8,
    emissive: 0x0284c7,
    bright: 0xbae6fd,
    deep: 0x0369a1,
    ring: 0xfcd34d,
    light: 0xf0f9ff,
  },
  point: {
    main: 0x60a5fa,
    emissive: 0x2563eb,
    bright: 0xdbeafe,
    deep: 0x1d4ed8,
    ring: 0xfbbf24,
    light: 0xfffbeb,
  },
};

function getPalette(key) {
  return THREE_RARITY_PALETTE[key] || THREE_RARITY_PALETTE.point;
}

/**
 * Clamps any normalized progress / spline parameter strictly into [0, 1].
 */
export function clamp01(value) {
  if (!Number.isFinite(value)) return 0;
  if (value <= 0) return 0;
  if (value >= 1) return 1;
  return value;
}

function disposeScene(scene) {
  scene.traverse((obj) => {
    if (obj.geometry) {
      obj.geometry.dispose();
    }
    if (obj.material) {
      const mats = Array.isArray(obj.material) ? obj.material : [obj.material];
      mats.forEach((m) => {
        if (m.map) m.map.dispose();
        m.dispose();
      });
    }
  });
}

/**
 * Procedurally synthesizes a crisp 4-point Genshin Primogem Star Flare DataTexture
 * in pure math (64x64 RGBA) for soft additive starlight bloom, sparks, and lens flares.
 */
function createStarFlareTexture(size = 64) {
  const data = new Uint8Array(size * size * 4);
  const half = (size - 1) * 0.5;

  for (let y = 0; y < size; y += 1) {
    const ny = (y - half) / half;
    for (let x = 0; x < size; x += 1) {
      const nx = (x - half) / half;
      const dist = Math.hypot(nx, ny);

      // Soft circular optical glow + sharp 4-point star spikes + subtle diagonal glints
      const core = Math.exp(-dist * dist * 8.5);
      const horiz = Math.exp(-Math.abs(ny) * 17.0) * Math.exp(-Math.abs(nx) * 2.5);
      const vert = Math.exp(-Math.abs(nx) * 17.0) * Math.exp(-Math.abs(ny) * 2.5);
      const diagA = Math.exp(-Math.abs(nx - ny) * 24.0) * Math.exp(-dist * 4.2) * 0.38;
      const diagB = Math.exp(-Math.abs(nx + ny) * 24.0) * Math.exp(-dist * 4.2) * 0.38;

      const edgeFade = clamp01(1 - Math.pow(dist, 2.4));
      const intensity = clamp01((core * 1.05 + horiz * 0.88 + vert * 0.88 + diagA + diagB) * edgeFade);

      const idx = (y * size + x) * 4;
      data[idx] = 255;
      data[idx + 1] = 255;
      data[idx + 2] = 255;
      data[idx + 3] = Math.round(intensity * 255);
    }
  }

  const tex = new THREE.DataTexture(data, size, size, THREE.RGBAFormat);
  tex.magFilter = THREE.LinearFilter;
  tex.minFilter = THREE.LinearFilter;
  tex.needsUpdate = true;
  return tex;
}

/**
 * Builds a multi-faceted bipyramid gemstone BufferGeometry with crisp flat-shaded normals.
 */
function createCrystalGemGeometry(radius = 1, height = 2.35, segments = 8) {
  const upper = new THREE.ConeGeometry(radius, height * 0.42, segments);
  upper.translate(0, height * 0.21, 0);

  const lower = new THREE.ConeGeometry(radius, height * 0.58, segments);
  lower.rotateX(Math.PI);
  lower.translate(0, -height * 0.29, 0);

  const gA = upper.toNonIndexed();
  const gB = lower.toNonIndexed();
  const posA = gA.getAttribute('position').array;
  const posB = gB.getAttribute('position').array;
  const merged = new Float32Array(posA.length + posB.length);
  merged.set(posA, 0);
  merged.set(posB, posA.length);

  upper.dispose();
  lower.dispose();
  gA.dispose();
  gB.dispose();

  const out = new THREE.BufferGeometry();
  out.setAttribute('position', new THREE.BufferAttribute(merged, 3));
  out.computeVertexNormals();
  return out;
}

/**
 * Constructs a multi-layered Refractive Celestial Diamond Assembly:
 *  1) High-IOR Glass/Diamond Outer Shell (`MeshPhysicalMaterial` with IBL reflections + iridescence)
 *  2) Incandescent Additive Inner Soul Core (glowing from inside the crystal)
 *  3) Crisp Specular Facet Ridge Lines (`EdgesGeometry` with `AdditiveBlending`)
 *  4) Optical Star-Bloom Halo Sprite
 */
function createRefractiveGemAssembly({
  radius = 1,
  height = 2.4,
  segments = 8,
  shellColor = 0xfef08a,
  attenuationColor = 0xf59e0b,
  coreColor = 0xffffff,
  starTex,
  haloScale = 4.2,
}) {
  const group = new THREE.Group();
  const gemGeo = createCrystalGemGeometry(radius, height, segments);

  // 1. Refractive Diamond Outer Prism
  const shellMat = new THREE.MeshPhysicalMaterial({
    color: shellColor,
    emissive: attenuationColor,
    emissiveIntensity: 0.18,
    metalness: 0.06,
    roughness: 0.05,
    transmission: 0.82,
    ior: 2.38,
    thickness: radius * 2.2,
    attenuationColor: new THREE.Color(attenuationColor),
    attenuationDistance: radius * 1.8,
    iridescence: 0.52,
    iridescenceIOR: 1.75,
    clearcoat: 1.0,
    clearcoatRoughness: 0.04,
    envMapIntensity: 2.5,
    flatShading: true,
  });
  const shellMesh = new THREE.Mesh(gemGeo, shellMat);
  group.add(shellMesh);

  // 2. Incandescent Inner Energy Core (burns brightly inside the refractive shell)
  const coreMat = new THREE.MeshBasicMaterial({
    color: coreColor,
    transparent: true,
    opacity: 0.9,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
  });
  const innerCore = new THREE.Mesh(gemGeo, coreMat);
  innerCore.scale.setScalar(0.5);
  group.add(innerCore);

  // 3. Razor-Sharp Specular Facet Edge Highlights
  const edgesGeo = new THREE.EdgesGeometry(gemGeo, 12);
  const edgesMat = new THREE.LineBasicMaterial({
    color: 0xffffff,
    transparent: true,
    opacity: 0.72,
    blending: THREE.AdditiveBlending,
  });
  const edgeLines = new THREE.LineSegments(edgesGeo, edgesMat);
  edgeLines.scale.setScalar(1.003);
  group.add(edgeLines);

  // 4. Soft Additive Optical Bloom Halo
  if (starTex) {
    const haloMat = new THREE.SpriteMaterial({
      map: starTex,
      color: shellColor,
      transparent: true,
      opacity: 0.68,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const haloSprite = new THREE.Sprite(haloMat);
    haloSprite.scale.set(haloScale, haloScale, 1);
    group.add(haloSprite);
  }

  return { group, shellMat, coreMat, edgesMat };
}

/**
 * 1. REAL-TIME 3D WEBGL METEOR FLIGHT & GENSHIN CELESTIAL SUPERNOVA CUTSCENE!
 * Zero chunky plastic blocks: uses HDR environment reflections, refractive diamond core,
 * silky additive starlight trails, anamorphic lens-flare cross, volumetric light rays,
 * and needle-sharp streaking light-spears aligned to velocity.
 */
export function GachaThreeMeteorScene({
  highestRarityKey = 'point',
  pullCount = 1,
  onComplete,
}) {
  const mountRef = useRef(null);
  const onCompleteRef = useRef(onComplete);

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  useEffect(() => {
    const container = mountRef.current;
    if (!container) return undefined;

    const palette = getPalette(highestRarityKey);
    let renderer;
    try {
      renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    } catch {
      const fallbackTimer = window.setTimeout(() => onCompleteRef.current?.(), 600);
      return () => window.clearTimeout(fallbackTimer);
    }

    const rect = container.getBoundingClientRect();
    const initW = Math.max(320, rect.width || 800);
    const initH = Math.max(260, rect.height || 440);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setSize(initW, initH);
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    container.appendChild(renderer.domElement);

    const scene = new THREE.Scene();

    // HDR Studio Environment Map so metallic gold & refractive crystal gleam like real jewelry!
    const pmremGenerator = new THREE.PMREMGenerator(renderer);
    const envRT = pmremGenerator.fromScene(new RoomEnvironment(), 0.035);
    scene.environment = envRT.texture;
    pmremGenerator.dispose();

    const camera = new THREE.PerspectiveCamera(48, initW / initH, 0.1, 180);
    camera.position.set(0, 1.1, 16);

    const starTex = createStarFlareTexture(64);

    // Subtle crisp lighting (let HDR envMap + additive energy do the heavy lifting, avoiding plastic wash)
    const ambient = new THREE.AmbientLight(0xfff5e0, 0.35);
    scene.add(ambient);

    const keyLight = new THREE.DirectionalLight(0xfffbeb, 2.2);
    keyLight.position.set(7, 12, 10);
    scene.add(keyLight);

    const gateLight = new THREE.PointLight(0xfbbf24, 3.2, 32);
    gateLight.position.set(0, 0, 0);
    scene.add(gateLight);

    // 3D Starlight Field with Soft Glowing Star Sprites (NOT square pixels!)
    const starCount = 380;
    const starPositions = new Float32Array(starCount * 3);
    for (let i = 0; i < starCount; i += 1) {
      starPositions[i * 3] = (Math.random() - 0.5) * 66;
      starPositions[i * 3 + 1] = (Math.random() - 0.5) * 40;
      starPositions[i * 3 + 2] = -Math.random() * 72 + 12;
    }
    const starGeo = new THREE.BufferGeometry();
    starGeo.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    const starMat = new THREE.PointsMaterial({
      map: starTex,
      color: 0xfff9e6,
      size: 0.85,
      transparent: true,
      opacity: 0.9,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const starField = new THREE.Points(starGeo, starMat);
    scene.add(starField);

    // 3D Gyroscopic 24K Mirror-Gold Celestia Armillary Gate at Origin
    const gateGroup = new THREE.Group();
    scene.add(gateGroup);

    const goldRingMat = new THREE.MeshPhysicalMaterial({
      color: 0xfcd34d,
      emissive: 0x92400e,
      emissiveIntensity: 0.22,
      metalness: 0.94,
      roughness: 0.12,
      clearcoat: 0.8,
      envMapIntensity: 2.2,
    });
    const innerRingMat = new THREE.MeshPhysicalMaterial({
      color: 0xfef08a,
      emissive: 0xb45309,
      emissiveIntensity: 0.35,
      metalness: 0.88,
      roughness: 0.1,
      envMapIntensity: 2.4,
    });

    const ring1 = new THREE.Mesh(new THREE.TorusGeometry(4.2, 0.055, 20, 96), goldRingMat);
    const ring2 = new THREE.Mesh(new THREE.TorusGeometry(3.35, 0.042, 18, 84), innerRingMat);
    const ring3 = new THREE.Mesh(new THREE.TorusGeometry(2.55, 0.032, 16, 72), goldRingMat);
    ring1.rotation.set(0.55, 0.35, 0);
    ring2.rotation.set(-0.45, 0.65, 0.25);
    ring3.rotation.set(1.05, -0.25, 0.4);
    gateGroup.add(ring1, ring2, ring3);

    // Delicate Glowing Star Nodes on Outer Ring (instead of bulky octahedron studs)
    for (let i = 0; i < 8; i += 1) {
      const a = (i / 8) * Math.PI * 2;
      const nodeMat = new THREE.SpriteMaterial({
        map: starTex,
        color: 0xfffbeb,
        transparent: true,
        opacity: 0.92,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });
      const node = new THREE.Sprite(nodeMat);
      node.scale.set(0.95, 0.95, 1);
      node.position.set(Math.cos(a) * 4.2, Math.sin(a) * 4.2, 0);
      ring1.add(node);
    }

    // Incandescent Genshin Shooting Star Comet Head + Silky Additive Star-Trail Sprites
    const cometGroup = new THREE.Group();
    scene.add(cometGroup);

    const cometCoreSpriteMat = new THREE.SpriteMaterial({
      map: starTex,
      color: 0xffffff,
      transparent: true,
      opacity: 1,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const cometCoreSprite = new THREE.Sprite(cometCoreSpriteMat);
    cometCoreSprite.scale.set(3.8, 3.8, 1);

    const cometAuraSpriteMat = new THREE.SpriteMaterial({
      map: starTex,
      color: 0x7dd3fc,
      transparent: true,
      opacity: 0.92,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const cometAuraSprite = new THREE.Sprite(cometAuraSpriteMat);
    cometAuraSprite.scale.set(7.2, 7.2, 1);
    cometGroup.add(cometAuraSprite, cometCoreSprite);

    // Silky Glowing Comet Tail Ribbon (52 overlapping additive star-glow nodes — zero plastic beads!)
    const wakeNodes = [];
    for (let i = 0; i < 52; i += 1) {
      const ratio = 1 - i / 52;
      const wMat = new THREE.SpriteMaterial({
        map: starTex,
        color: i < 10 ? 0xffffff : 0x7dd3fc,
        transparent: true,
        opacity: ratio * 0.78,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });
      const sp = new THREE.Sprite(wMat);
      const s = 0.45 + ratio * 2.5;
      sp.scale.set(s, s, 1);
      scene.add(sp);
      wakeNodes.push({
        sprite: sp,
        mat: wMat,
        lag: (i + 1) * 0.0095,
        phase: i * 0.45,
        radius: (1 - ratio) * 0.42,
        baseOpacity: ratio * 0.78,
      });
    }

    // Companion Luminous Shooting Stars for 10x Pull
    const companions = [];
    if (pullCount > 1) {
      for (let c = 0; c < 9; c += 1) {
        const cMat = new THREE.SpriteMaterial({
          map: starTex,
          color: 0xe0f2fe,
          transparent: true,
          opacity: 0.9,
          blending: THREE.AdditiveBlending,
          depthWrite: false,
        });
        const sp = new THREE.Sprite(cMat);
        sp.scale.set(2.1, 2.1, 1);
        scene.add(sp);
        companions.push({
          sprite: sp,
          lag: 0.045 + c * 0.03,
          angle: (c / 9) * Math.PI * 2,
          orbitR: 1.35 + (c % 3) * 0.55,
        });
      }
    }

    // Central Refractive Diamond Core Assembly (Phase B)
    const coreGem = createRefractiveGemAssembly({
      radius: 1.18,
      height: 2.85,
      segments: 8,
      shellColor: palette.bright,
      attenuationColor: palette.main,
      coreColor: 0xffffff,
      starTex,
      haloScale: 6.4,
    });
    coreGem.group.visible = false;
    scene.add(coreGem.group);

    // Phase C Supernova Detonation Layers (Pure Celestial Energy + Needle Light-Spears + Stardust!)
    // 1) Anamorphic Horizontal Lens-Flare + Blinding Central Star-Cross
    const flashStarMat = new THREE.SpriteMaterial({
      map: starTex,
      color: 0xffffff,
      transparent: true,
      opacity: 0,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const flashStarSprite = new THREE.Sprite(flashStarMat);
    scene.add(flashStarSprite);

    const anamorphicMat = new THREE.SpriteMaterial({
      map: starTex,
      color: palette.bright,
      transparent: true,
      opacity: 0,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const anamorphicSprite = new THREE.Sprite(anamorphicMat);
    scene.add(anamorphicSprite);

    // 2) Twin Hairline Additive Celestial Shockwave Ripples
    const shockGeo = new THREE.RingGeometry(0.986, 1.0, 96);
    const shockMat1 = new THREE.MeshBasicMaterial({
      color: 0xfffbeb,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const shockMat2 = new THREE.MeshBasicMaterial({
      color: palette.main,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const shockRing1 = new THREE.Mesh(shockGeo, shockMat1);
    const shockRing2 = new THREE.Mesh(shockGeo, shockMat2);
    shockRing1.rotation.x = 0.55;
    shockRing2.rotation.set(-0.4, 0.35, 0);
    scene.add(shockRing1, shockRing2);

    // 3) 38 Ultra-Fine Optical Diffraction Light Filaments (aligned radially along velocity)
    const spearGeo = createCrystalGemGeometry(0.022, 1.95, 4);
    spearGeo.rotateX(Math.PI * 0.5);
    const lightSpears = [];
    for (let s = 0; s < 38; s += 1) {
      const sMat = new THREE.MeshBasicMaterial({
        color: s % 3 === 0 ? 0xfffbeb : s % 2 === 0 ? palette.bright : palette.main,
        transparent: true,
        opacity: 0.78,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });
      const mesh = new THREE.Mesh(spearGeo, sMat);
      mesh.visible = false;

      const theta = (s / 38) * Math.PI * 2 + (Math.random() - 0.5) * 0.22;
      const zBias = (Math.random() - 0.35) * 0.85;
      const dir = new THREE.Vector3(
        Math.cos(theta) * Math.sqrt(Math.max(0.05, 1 - zBias * zBias)),
        Math.sin(theta) * Math.sqrt(Math.max(0.05, 1 - zBias * zBias)),
        zBias,
      ).normalize();
      const speed = 13 + Math.random() * 17;
      const lenScale = 0.55 + Math.random() * 0.85;
      mesh.scale.set(1, 1, lenScale);

      scene.add(mesh);
      lightSpears.push({
        mesh,
        mat: sMat,
        vel: dir.multiplyScalar(speed),
        lenScale,
      });
    }

    // 4) 150 Exploding Glowing Stardust Embers (Additive Star-Flare Sprites with drag physics!)
    const emberCount = 150;
    const emberPositions = new Float32Array(emberCount * 3);
    const emberVelocities = [];
    for (let e = 0; e < emberCount; e += 1) {
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(2 * Math.random() - 1);
      const speed = 4 + Math.random() * 16;
      emberVelocities.push({
        vx: Math.sin(phi) * Math.cos(theta) * speed,
        vy: Math.sin(phi) * Math.sin(theta) * speed,
        vz: Math.cos(phi) * speed + 3.0,
      });
    }
    const emberGeo = new THREE.BufferGeometry();
    emberGeo.setAttribute('position', new THREE.BufferAttribute(emberPositions, 3));
    const emberMat = new THREE.PointsMaterial({
      map: starTex,
      color: palette.bright,
      size: 1.35,
      transparent: true,
      opacity: 0,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    });
    const emberPoints = new THREE.Points(emberGeo, emberMat);
    emberPoints.visible = false;
    scene.add(emberPoints);

    // 3D Curved Comet Spline
    const curve = new THREE.CatmullRomCurve3([
      new THREE.Vector3(19, 10.5, -34),
      new THREE.Vector3(9.5, 4.8, -16),
      new THREE.Vector3(3.2, 1.4, -5.5),
      new THREE.Vector3(0, 0, 0),
    ]);

    const onResize = () => {
      if (!container) return;
      const r = container.getBoundingClientRect();
      const w = Math.max(320, r.width);
      const h = Math.max(260, r.height);
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
    };
    window.addEventListener('resize', onResize);

    const totalMs = 2350;
    let startAt = null;
    let prevNow = null;
    let ignited = false;
    let shattered = false;
    let rafId = 0;

    const tick = (now) => {
      if (startAt === null) {
        startAt = now;
        prevNow = now;
      }
      const dt = clamp01((now - prevNow) / 1000) > 0.05 ? 0.05 : Math.max(0, (now - prevNow) / 1000);
      prevNow = now;
      const elapsed = Math.max(0, now - startAt);
      const p = clamp01(elapsed / totalMs);

      try {
        starField.position.z = p * 20;
        starField.rotation.z = p * 0.16;

        // Mid-flight Rarity Color Ignition
        if (p >= 0.33 && !ignited) {
          ignited = true;
          cometAuraSpriteMat.color.setHex(palette.main);
          cometAuraSprite.scale.set(9.2, 9.2, 1);
          gateLight.color.setHex(palette.main);
          innerRingMat.emissive.setHex(palette.emissive);
          wakeNodes.forEach((wn, idx) => {
            wn.mat.color.setHex(idx < 12 ? 0xffffff : palette.bright);
          });
        }

        const ringSpeed = p < 0.52 ? 1.1 + p * 3.2 : 4.8;
        ring1.rotation.x += dt * ringSpeed * 0.9;
        ring1.rotation.y += dt * ringSpeed * 1.1;
        ring2.rotation.y -= dt * ringSpeed * 1.35;
        ring2.rotation.z += dt * ringSpeed * 0.85;
        ring3.rotation.x -= dt * ringSpeed * 1.2;
        ring3.rotation.z -= dt * ringSpeed * 1.05;

        if (p < 0.52) {
          // PHASE A: Luminous Celestial Shooting Star hurtling into the 3D Golden Gate
          const flightT = clamp01(p / 0.52);
          const eased = clamp01(1 - Math.pow(1 - flightT, 2.15));
          const pos = curve.getPointAt(eased);

          cometGroup.position.copy(pos);
          const pulse = 1 + Math.sin(elapsed * 0.03) * 0.12;
          cometCoreSprite.scale.setScalar(3.8 * pulse);

          wakeNodes.forEach((wn) => {
            const wt = clamp01(eased - wn.lag);
            const wPos = curve.getPointAt(wt);
            const swirl = elapsed * 0.012 + wn.phase;
            wn.sprite.visible = eased - wn.lag > 0.008;
            wn.sprite.position.set(
              wPos.x + Math.cos(swirl) * wn.radius,
              wPos.y + Math.sin(swirl) * wn.radius,
              wPos.z,
            );
          });

          companions.forEach((comp) => {
            const ct = clamp01((flightT - comp.lag) / (1 - comp.lag));
            comp.sprite.visible = ct > 0 && ct < 0.99;
            if (comp.sprite.visible) {
              const ce = clamp01(1 - Math.pow(1 - ct, 2));
              const cPos = curve.getPointAt(ce);
              const ang = comp.angle + ce * 5.2;
              const spread = (1 - ce * 0.75) * comp.orbitR;
              comp.sprite.position.set(
                cPos.x + Math.cos(ang) * spread,
                cPos.y + Math.sin(ang) * spread,
                cPos.z,
              );
            }
          });

          camera.position.set(
            Math.sin(p * 3.0) * 1.3,
            1.2 - flightT * 0.9,
            16.2 - flightT * 5.0,
          );
          camera.lookAt(pos.x * 0.35, pos.y * 0.35, pos.z * 0.25);
        } else if (p < 0.75) {
          // PHASE B: Refractive Diamond Core Crystallizes & Resonates Inside the Gate
          cometGroup.visible = false;
          wakeNodes.forEach((wn) => {
            wn.sprite.visible = false;
          });
          companions.forEach((c) => {
            c.sprite.visible = false;
          });

          const coreT = clamp01((p - 0.52) / 0.23);
          coreGem.group.visible = true;
          const crystalScale = 0.38 + Math.sin(coreT * Math.PI * 0.5) * 0.88;
          coreGem.group.scale.setScalar(crystalScale);
          coreGem.group.rotation.y += dt * 7.2;
          coreGem.group.rotation.z = Math.sin(coreT * 6) * 0.14;
          coreGem.coreMat.opacity = 0.75 + coreT * 0.25;

          shockMat1.opacity = (1 - coreT) * 0.88;
          shockRing1.scale.setScalar(1.1 + coreT * 8.2);

          camera.position.set(
            Math.sin(coreT * 2.0) * 0.65,
            0.3 * (1 - coreT),
            11.2 - coreT * 3.5,
          );
          camera.lookAt(0, 0, 0);
        } else {
          // PHASE C: Genshin Anamorphic Supernova + Streaking Light-Spears + Stardust Fireworks!
          if (!shattered) {
            shattered = true;
            coreGem.group.visible = false;
            emberPoints.visible = true;
            const posAttr = emberGeo.getAttribute('position');
            for (let i = 0; i < emberCount * 3; i += 1) {
              posAttr.array[i] = 0;
            }
            posAttr.needsUpdate = true;

            lightSpears.forEach((ls) => {
              ls.mesh.visible = true;
              ls.mesh.position.set(0, 0, 0);
              ls.mesh.lookAt(ls.vel);
            });
          }

          const burstT = clamp01((p - 0.75) / 0.25);
          const fadeOut = clamp01(1 - Math.pow(burstT, 1.45));

          gateGroup.scale.setScalar(1 + burstT * 1.9);

          // Sleek Anamorphic Star-Cross Lens Flare (crisp optical bloom without whiteout)
          const flareEnv = burstT < 0.24 ? burstT / 0.24 : clamp01(1 - (burstT - 0.24) / 0.76);
          flashStarMat.opacity = flareEnv * 0.56;
          flashStarSprite.scale.setScalar(4.5 + burstT * 11.5);

          anamorphicMat.opacity = flareEnv * 0.72;
          anamorphicSprite.scale.set(14 + burstT * 26, 0.95 + flareEnv * 1.15, 1);

          // Twin Hairline Celestial Halo Ripples
          shockMat1.opacity = fadeOut * 0.72;
          shockRing1.scale.setScalar(2.0 + burstT * 13);
          shockMat2.opacity = fadeOut * 0.65;
          shockRing2.scale.setScalar(1.4 + burstT * 9.8);

          // Supersonic Optical Light Filaments
          lightSpears.forEach((ls) => {
            ls.mesh.position.addScaledVector(ls.vel, dt);
            ls.mat.opacity = fadeOut * 0.75;
            ls.mesh.scale.set(fadeOut, fadeOut, ls.lenScale * (1 + burstT * 1.4));
          });

          // Glowing Stardust Embers with Aerodynamic Drag
          const posAttr = emberGeo.getAttribute('position');
          const arr = posAttr.array;
          const drag = Math.pow(0.94, dt * 60);
          for (let e = 0; e < emberCount; e += 1) {
            const v = emberVelocities[e];
            v.vx *= drag;
            v.vy *= drag;
            v.vz *= drag;
            arr[e * 3] += v.vx * dt;
            arr[e * 3 + 1] += v.vy * dt;
            arr[e * 3 + 2] += v.vz * dt;
          }
          posAttr.needsUpdate = true;
          emberMat.opacity = fadeOut * 0.95;

          camera.position.z = 7.7 - burstT * 2.8;
          camera.lookAt(0, 0, 0);
        }

        renderer.render(scene, camera);
      } catch {
        onCompleteRef.current?.();
        return;
      }

      if (p < 1) {
        rafId = window.requestAnimationFrame(tick);
      } else {
        onCompleteRef.current?.();
      }
    };

    rafId = window.requestAnimationFrame(tick);

    return () => {
      window.cancelAnimationFrame(rafId);
      window.removeEventListener('resize', onResize);
      scene.environment = null;
      envRT.dispose();
      starTex.dispose();
      spearGeo.dispose();
      disposeScene(scene);
      renderer.dispose();
      if (renderer.domElement && renderer.domElement.parentNode === container) {
        container.removeChild(renderer.domElement);
      }
    };
  }, [highestRarityKey, pullCount]);

  return (
    <div
      ref={mountRef}
      className="pointer-events-none absolute inset-0 z-10 h-full w-full"
      aria-hidden="true"
    />
  );
}

/**
 * 2. REAL-TIME 3D WEBGL REVEAL STAGE:
 * Uses RoomEnvironment HDR IBL so gold gleams like 24K mirror-gold and crystals
 * refract like true faceted Swarovski/Primogem diamonds, surrounded by soft
 * additive twinkling stardust motes (zero plastic beads!).
 */
export function GachaThreeRevealStage({
  rarityKey = 'point',
  itemMode = 'mascot', // 'crystal' | 'points' | 'mascot'
  pointsAmount = 20,
}) {
  const mountRef = useRef(null);

  useEffect(() => {
    const container = mountRef.current;
    if (!container) return undefined;

    let renderer;
    try {
      renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    } catch {
      return undefined;
    }

    const palette = getPalette(rarityKey);
    const rect = container.getBoundingClientRect();
    const initW = Math.max(320, rect.width || 800);
    const initH = Math.max(260, rect.height || 440);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setSize(initW, initH);
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.16;
    container.appendChild(renderer.domElement);

    const scene = new THREE.Scene();

    const pmremGenerator = new THREE.PMREMGenerator(renderer);
    const envRT = pmremGenerator.fromScene(new RoomEnvironment(), 0.035);
    scene.environment = envRT.texture;
    pmremGenerator.dispose();

    const starTex = createStarFlareTexture(64);

    const camera = new THREE.PerspectiveCamera(42, initW / initH, 0.1, 100);
    camera.position.set(0, 1.35, 10.5);
    camera.lookAt(0, 0.15, 0);

    const ambient = new THREE.AmbientLight(0xfffbeb, 0.42);
    scene.add(ambient);

    const keyLight = new THREE.DirectionalLight(0xffffff, 2.3);
    keyLight.position.set(5, 8, 7);
    scene.add(keyLight);

    const rimLight = new THREE.PointLight(palette.main, 3.4, 24);
    rimLight.position.set(-4, 3, 4);
    scene.add(rimLight);

    // Floating 3D 24K Mirror-Gold Rune Rings Hovering Directly Above the Painted Celestia Altar
    const daisGroup = new THREE.Group();
    daisGroup.position.set(0, -1.55, 0);
    scene.add(daisGroup);

    const goldTrimMat = new THREE.MeshPhysicalMaterial({
      color: 0xfcd34d,
      emissive: 0x92400e,
      emissiveIntensity: 0.22,
      metalness: 0.95,
      roughness: 0.11,
      clearcoat: 0.9,
      envMapIntensity: 2.3,
    });
    const rarityGlowMat = new THREE.MeshBasicMaterial({
      color: palette.bright,
      transparent: true,
      opacity: 0.85,
      blending: THREE.AdditiveBlending,
    });

    const daisGoldRim = new THREE.Mesh(new THREE.TorusGeometry(2.65, 0.042, 18, 84), goldTrimMat);
    daisGoldRim.rotation.x = Math.PI * 0.5;
    const daisRuneRing = new THREE.Mesh(new THREE.TorusGeometry(1.98, 0.024, 14, 72), rarityGlowMat);
    daisRuneRing.rotation.x = Math.PI * 0.5;
    daisRuneRing.position.y = 0.06;
    daisGroup.add(daisGoldRim, daisRuneRing);

    // Delicate 3D Gyroscopic Filigree Rings
    const haloGroup = new THREE.Group();
    haloGroup.position.set(0, 0.48, -0.85);
    scene.add(haloGroup);

    const outerHalo = new THREE.Mesh(new THREE.TorusGeometry(2.82, 0.032, 18, 84), goldTrimMat);
    outerHalo.rotation.set(0.32, 0.25, 0);
    const innerHalo = new THREE.Mesh(new THREE.TorusGeometry(2.22, 0.024, 16, 72), rarityGlowMat);
    innerHalo.rotation.set(-0.38, -0.3, 0.2);
    haloGroup.add(outerHalo, innerHalo);

    // Softly Twinkling Additive Starlight Motes Orbiting in 3D Depth (NO plastic blocks!)
    const orbitGroup = new THREE.Group();
    orbitGroup.position.set(0, 0.45, 0);
    scene.add(orbitGroup);

    const orbiters = [];
    for (let i = 0; i < 22; i += 1) {
      const mMat = new THREE.SpriteMaterial({
        map: starTex,
        color: i % 2 === 0 ? 0xffffff : palette.bright,
        transparent: true,
        opacity: 0.85,
        blending: THREE.AdditiveBlending,
        depthWrite: false,
      });
      const sp = new THREE.Sprite(mMat);
      const sz = 0.36 + (i % 4) * 0.14;
      sp.scale.set(sz, sz, 1);
      orbitGroup.add(sp);
      orbiters.push({
        sprite: sp,
        mat: mMat,
        baseSize: sz,
        angle: (i / 22) * Math.PI * 2,
        radius: 2.35 + (i % 3) * 0.58,
        yAmp: 0.55 + (i % 4) * 0.22,
        speed: 0.42 + (i % 3) * 0.16,
      });
    }

    // Centerpiece 3D Hero Object (Refractive Diamond Crystal OR Proof-Gold & Cut-Gem Medallion)
    const heroGroup = new THREE.Group();
    heroGroup.position.set(0, 0.62, 0.55);
    heroGroup.scale.setScalar(0.05);
    scene.add(heroGroup);

    if (itemMode === 'crystal') {
      const gem = createRefractiveGemAssembly({
        radius: 0.96,
        height: 2.4,
        segments: 8,
        shellColor: 0xfef08a,
        attenuationColor: 0xd97706,
        coreColor: 0xffffff,
        starTex,
        haloScale: 4.8,
      });
      const orbitRing = new THREE.Mesh(new THREE.TorusGeometry(1.42, 0.025, 16, 64), goldTrimMat);
      orbitRing.rotation.set(1.15, 0.3, 0);
      heroGroup.add(gem.group, orbitRing);
    } else if (itemMode === 'points') {
      const isGoldCoin = pointsAmount >= 150;
      const isMidCoin = pointsAmount >= 60 && pointsAmount < 150;

      const coinMetalMat = new THREE.MeshPhysicalMaterial({
        color: isGoldCoin ? 0xfcd34d : 0xfde68a,
        emissive: 0x92400e,
        emissiveIntensity: 0.15,
        metalness: 0.96,
        roughness: 0.12,
        clearcoat: 1.0,
        clearcoatRoughness: 0.05,
        envMapIntensity: 2.4,
      });
      const coinInlayMat = new THREE.MeshPhysicalMaterial({
        color: isGoldCoin ? 0xb45309 : isMidCoin ? 0x0369a1 : 0x1e40af,
        metalness: 0.35,
        roughness: 0.16,
        clearcoat: 1.0,
        iridescence: 0.4,
        envMapIntensity: 1.9,
      });
      const jewelCrystalMat = new THREE.MeshPhysicalMaterial({
        color: isGoldCoin ? 0xfef08a : 0xbae6fd,
        emissive: isGoldCoin ? 0xd97706 : 0x0284c7,
        emissiveIntensity: 0.28,
        metalness: 0.08,
        roughness: 0.04,
        transmission: 0.72,
        ior: 2.35,
        thickness: 1.2,
        clearcoat: 1.0,
        envMapIntensity: 2.5,
        flatShading: true,
      });

      const body = new THREE.Mesh(new THREE.CylinderGeometry(1.12, 1.12, 0.22, 64), coinMetalMat);
      body.rotation.x = Math.PI * 0.5;

      const facePlate = new THREE.Mesh(new THREE.CylinderGeometry(0.92, 0.92, 0.245, 64), coinInlayMat);
      facePlate.rotation.x = Math.PI * 0.5;

      const outerBezel = new THREE.Mesh(new THREE.TorusGeometry(1.1, 0.075, 20, 64), coinMetalMat);
      const innerBeadRing = new THREE.Mesh(new THREE.TorusGeometry(0.88, 0.022, 14, 56), coinMetalMat);
      innerBeadRing.position.z = 0.125;
      const innerBeadRingBack = innerBeadRing.clone();
      innerBeadRingBack.position.z = -0.125;

      // Cut Refractive Star-Crystal Inlaid on Both Sides of the Proof Gold Medallion
      const vSpire = new THREE.Mesh(new THREE.OctahedronGeometry(0.56, 0), jewelCrystalMat);
      vSpire.scale.set(0.32, 1.18, 0.36);
      const hSpire = new THREE.Mesh(new THREE.OctahedronGeometry(0.56, 0), jewelCrystalMat);
      hSpire.scale.set(1.18, 0.32, 0.36);

      const glintSprite = new THREE.Sprite(
        new THREE.SpriteMaterial({
          map: starTex,
          color: 0xffffff,
          transparent: true,
          opacity: 0.78,
          blending: THREE.AdditiveBlending,
          depthWrite: false,
        }),
      );
      glintSprite.scale.set(2.4, 2.4, 1);

      heroGroup.add(body, facePlate, outerBezel, innerBeadRing, innerBeadRingBack, vSpire, hSpire, glintSprite);
    }

    const onResize = () => {
      if (!container) return;
      const r = container.getBoundingClientRect();
      const w = Math.max(320, r.width);
      const h = Math.max(260, r.height);
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
    };
    window.addEventListener('resize', onResize);

    let startAt = null;
    let prevNow = null;
    let rafId = 0;

    const tick = (now) => {
      if (startAt === null) {
        startAt = now;
        prevNow = now;
      }
      const dt = Math.min(0.05, Math.max(0, (now - prevNow) / 1000));
      prevNow = now;
      const t = Math.max(0, (now - startAt) / 1000);

      const introT = clamp01(t / 0.56);
      const springScale =
        introT < 1
          ? 1 - Math.pow(1 - introT, 3) * Math.cos(introT * Math.PI * 1.35)
          : 1;
      heroGroup.scale.setScalar(Math.max(0.05, springScale));

      if (itemMode !== 'mascot') {
        heroGroup.rotation.y = (1 - introT) * Math.PI * 1.4 + t * 1.05;
        heroGroup.position.y = 0.62 + Math.sin(t * 2.2) * 0.11;
      }

      daisGroup.rotation.y = t * 0.22;
      outerHalo.rotation.z = t * 0.34;
      outerHalo.rotation.y = 0.25 + Math.sin(t * 0.8) * 0.16;
      innerHalo.rotation.z = -t * 0.48;
      innerHalo.rotation.x = -0.38 + Math.cos(t * 0.9) * 0.14;

      orbiters.forEach((o, idx) => {
        const ang = o.angle + t * o.speed;
        o.sprite.position.set(
          Math.cos(ang) * o.radius,
          Math.sin(t * 1.5 + idx) * o.yAmp,
          Math.sin(ang) * o.radius * 0.65,
        );
        const tw = 0.65 + 0.35 * Math.sin(t * 4.2 + idx * 1.3);
        o.mat.opacity = tw * 0.9;
        o.sprite.scale.setScalar(o.baseSize * (0.85 + tw * 0.3));
      });

      try {
        renderer.render(scene, camera);
      } catch {
        return;
      }
      rafId = window.requestAnimationFrame(tick);
    };

    rafId = window.requestAnimationFrame(tick);

    return () => {
      window.cancelAnimationFrame(rafId);
      window.removeEventListener('resize', onResize);
      scene.environment = null;
      envRT.dispose();
      starTex.dispose();
      disposeScene(scene);
      renderer.dispose();
      if (renderer.domElement && renderer.domElement.parentNode === container) {
        container.removeChild(renderer.domElement);
      }
    };
  }, [rarityKey, itemMode, pointsAmount]);

  return (
    <div
      ref={mountRef}
      className="pointer-events-none absolute inset-0 z-10 h-full w-full"
      aria-hidden="true"
    />
  );
}
