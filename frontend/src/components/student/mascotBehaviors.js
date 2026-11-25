// mascotBehaviors.js
// Behavior profiles and dialogues for the 6 original page-mascot characters (100% English)

export const MASCOT_PROFILES = {
  fox: {
    dialogues: {
      perfect: [
        '100/100! All testcases passed green! I am so proud of you! 🦊🎉',
        'Immaculate code execution! You are a brilliant programmer! ⭐',
      ],
      good: [
        'Great score! Just a tiny optimization away from a perfect 100! 🦊✨',
        'Strong OOP fundamentals! Keep this great momentum going!',
      ],
      retry: [
        "Don't worry! Check your boundary conditions and try again! 💪🦊",
        'Little Fox is right here with you. Fix the bug and resubmit!',
      ],
      pet: [
        'Hehe, that tickles! Thank you for petting me! 🦊❤️',
        'Energized and ready to conquer the next lab with you!',
      ],
      idle: [
        'Have you completed all your streak missions today? 🦊',
        'Little Fox is keeping a watchful eye on your code!',
      ],
    },
  },
  bear: {
    dialogues: {
      perfect: [
        'A warm bear hug for your perfect 100! Phenomenal work! 🐻🎉',
        'Your architecture is rock-solid and reliable! Fantastic! ⭐',
      ],
      good: [
        'Very consistent score! Your logic is improving rapidly! 🐻🍯',
        'Solid work! A slight refactor will get you to 100!',
      ],
      retry: [
        'Grab a warm drink, take a deep breath, and debug calmly! 🐻',
        'Patience is key. We will crush those edge cases together!',
      ],
      pet: [
        'I love gentle head pats! Thank you so much! 🐻❤️',
        'Bear feels loved and supported. You are awesome!',
      ],
      idle: [
        'Slow and steady wins the race. Every line of code counts! 🐻',
        'Keep your head clear and stay focused on your goals.',
      ],
    },
  },
  panda: {
    dialogues: {
      perfect: [
        'Bamboo shoot feast celebration! Flawless 100 score! 🐼🎋🎉',
        'Masterful problem solving! You made it look effortless!',
      ],
      good: [
        'High score achieved! Munching bamboo in your honor! 🐼✨',
        'Smooth OOP implementation! Keep pushing for excellence!',
      ],
      retry: [
        'Do not rush! Even giant pandas take one step at a time! 🐼',
        'Review the stack trace together, you can definitely solve this!',
      ],
      pet: [
        'Rolling around happily! That feels so comforting! 🐼❤️',
        'Panda sends you endless focus and good study vibes!',
      ],
      idle: [
        'Coding is like eating bamboo: enjoy each bite thoughtfully! 🐼',
        'Panda is chewing bamboo quietly while watching you code.',
      ],
    },
  },
  penguin: {
    dialogues: {
      perfect: [
        'Even polar ice melts before your blazing 100 score! 🐧❄️',
        'Sliding smoothly through 100% of the operational testcases! ⛸️',
      ],
      good: [
        'Cool and composed! Clean OOP logic right there! 🐧✨',
        'Almost at the summit! You are sliding towards 100!',
      ],
      retry: [
        'NullPointerException? Let Penguin freeze that bug for you! ❄️',
        'Shake the frost off your wings and try once more!',
      ],
      pet: [
        'Refreshing and cool! Thank you for caring for me! 🐧❄️',
        'Flapping little wings in absolute joy and appreciation!',
      ],
      idle: [
        'Waddling along, ensuring all test suites are passing smoothly! 🐧',
        'Keep your code cool, clean, and well-structured.',
      ],
    },
  },
  bunny: {
    dialogues: {
      perfect: [
        'Hop hop hooray! 100 points! You are lightning fast! 🐰🥕🎉',
        'My long ears are twitching with joy! Incredible coding! ⭐',
      ],
      good: [
        'Hopping in the right direction! High score unlocked! 🐰✨',
        'Great speed and agility! Almost reached perfection!',
      ],
      retry: [
        'A little stumble is just a setup for a higher hop! 🐰💪',
        'Take a carrot break, breathe, and debug with a clear head!',
      ],
      pet: [
        'My soft bunny ears love gentle caresses! Thank you! 🐰❤️',
        'Hopping around in circles because you are so kind to me!',
      ],
      idle: [
        'Ready to leap over every difficult programming challenge! 🐰',
        'Bunny is listening closely to your keystrokes!',
      ],
    },
  },
  cat: {
    dialogues: {
      perfect: [
        'Purrrrrfect 100! Even a majestic cat must acknowledge your genius! 🐱👑✨',
        'Flawless execution! You have earned royal feline approval! 🐾🎉',
      ],
      good: [
        'Decent job, human. But I know you have the potential for 100! 🐱',
        'Not bad at all! Sharpen your claws and aim for the top!',
      ],
      retry: [
        'A minor bug? Knock it off the table like a proper cat! 🐱🐾',
        'Do not despair! Cats have 9 lives, programmers have unlimited submits!',
      ],
      pet: [
        'Purrrrr... you hit the exact right scratching spot! 🐱❤️',
        'Rubbing softly against your hand. You may pet me more!',
      ],
      idle: [
        'Cat is napping on your keyboard... wait, wake up and code! 🐱💤',
        'Watching your cursor move across the screen intently.',
      ],
    },
  },
  vit: {
    dialogues: {
      perfect: [
        'Quack quack quack! Rubber duck debugging delivered a 100! 🦆🌊🎉',
        'Gliding effortlessly on the water! Zero bugs beneath the surface! 🦆✨',
      ],
      good: [
        'Splashing around in celebration! Just paddle a bit closer to 100! 🦆',
        'Smooth as a duck swimming! Clean architecture all around!',
      ],
      retry: [
        'Quack! Talk through your code with the rubber duck and find the bug! 🦆💡',
        'Water off a duck back! Do not let compile errors weigh you down!',
      ],
      pet: [
        'Happy quacks and gentle feather wiggles! Thank you so much! 🦆❤️',
        'Floating peacefully. The duck sends you calming coding vibes!',
      ],
      idle: [
        'Quacking softly... Remember to talk to your rubber duck! 🦆',
        'Calm on the surface, paddling furiously underneath. That is programming!',
      ],
    },
  },
  hamster: {
    dialogues: {
      perfect: [
        'Squeak squeak! Spinning the wheel at max speed for your 100! 🐹🌻🎉',
        'Sunflower seed celebration! Everything passed with flying colors! 🐹✨',
      ],
      good: [
        'Stashing bonus points into chubby cheeks! Great work! 🐹',
        'Squeaking enthusiastically! Just a little push to reach 100!',
      ],
      retry: [
        'Do not squeak in sorrow! Take a sunflower seed and try again! 🐹💪',
        'Even tiny hamsters run miles every night! Keep pushing forward!',
      ],
      pet: [
        'Chubby cheeks vibrating with joy! That tickles so nicely! 🐹❤️',
        'Curling into a tiny warm fluffball in your palm!',
      ],
      idle: [
        'Hamster is nibbling sunflower seeds while watching you code! 🐹🌻',
        'Running fast on the wheel, spinning out optimized algorithms!',
      ],
    },
  },
  chuot_tui: {
    dialogues: {
      perfect: [
        'Boing boing! Bouncing to the stratosphere with a perfect 100! 🦘🎉',
        'A knockout kick to all software bugs! Champion performance! 🦘🥊✨',
      ],
      good: [
        'Leaping ahead of the competition! You are bouncing towards 100! 🦘',
        'Strong momentum! Store this high score safely in the pouch!',
      ],
      retry: [
        'Just a backward bounce to gain momentum! Leap forward again! 🦘💪',
        'Shake it off! Hop back on your feet and punch that bug out!',
      ],
      pet: [
        'A friendly kangaroo nuzzle! Thank you for being so caring! 🦘❤️',
        'Happy little hops! Kangaroo is cheering you on every step!',
      ],
      idle: [
        'Hopping along the outback of code! Ready for the next leap! 🦘',
        'Keep bounding forward. Persistence breaks any barrier.',
      ],
    },
  },
  dai_bang: {
    dialogues: {
      perfect: [
        'The Eagle soars above the clouds! A breathtaking 100/100! 🦅✨🎉',
        'Sharp vision and swift execution! No edge case escaped your keen eyes! 🦅⭐',
      ],
      good: [
        'Riding the thermals high! A splendid score, just glide towards 100! 🦅',
        'Target locked! You have almost reached perfection, strike now!',
      ],
      retry: [
        'Fold your wings, adjust your angle, and dive back in! 🦅💪',
        'Even the king of the skies circles before striking! Debug and resubmit!',
      ],
      pet: [
        'Smooth noble feathers! Eagle screeches gently in gratitude! 🦅❤️',
        'Flapping mighty wings with pride! Keep soaring higher!',
      ],
      idle: [
        'Surveying your code from the mountain peaks! 🦅',
        'Keep your eyes sharp and your algorithms soaring high.',
      ],
    },
  },
  cho_soi: {
    dialogues: {
      perfect: [
        'Awooooo! The lone wolf howls for your glorious 100 points! 🐺🌕🎉',
        'The pack runs free! Flawless execution across every single testcase! 🐺✨',
      ],
      good: [
        'Strong hunting instincts! Tracking down the scent of a perfect 100! 🐺',
        'The pack scents victory! Hunt down those last few testcases!',
      ],
      retry: [
        'A wounded wolf fights even fiercer! Hunt down that exception! 🐺💪',
        'Regroup with the pack! Debug calmly and conquer the challenge!',
      ],
      pet: [
        'A loyal wolf nuzzling your hand affectionately! Thank you! 🐺❤️',
        'Howling softly with happiness! You have earned the wolf trust!',
      ],
      idle: [
        'Guarding the codebase through the midnight hours! 🐺🌙',
        'Stay focused and run with the pack. Success takes persistence.',
      ],
    },
  },
  thanh_long: {
    dialogues: {
      perfect: [
        'Behold the Azure Dragon! Thunder roars for your perfect 100! 🐉⚡🎉',
        'Flawless mastery! You command the flow of bytecode like celestial lightning! 🐉✨',
      ],
      good: [
        'A majestic performance! Ascend higher to claim the ultimate 100! 🐉',
        'Winds of victory blow in your favor! Polish the final edge cases!',
      ],
      retry: [
        'Even ancient dragons must weather the tempest! Refactor and soar again! 🐉⚡',
        'Do not falter! Harness the dragon wrath and conquer those failing tests!',
      ],
      pet: [
        'The dragon scales shimmer with celestial light. Thank you, mortal coder! 🐉✨',
        'You have gained the favor of the Azure Dragon! May your code be blessed!',
      ],
      idle: [
        'Thanh Long watches over your IDE from the celestial heavens! 🐉',
        'Let wisdom guide your algorithms and honor the craft.',
      ],
    },
  },
  bach_ho: {
    dialogues: {
      perfect: [
        'The White Tiger roars! A lethal 100/100 strike with zero hesitation! 🐅⚔️🎉',
        'Absolute dominance! Not a single bug survived your sharp OOP claws! 🐅🔥',
      ],
      good: [
        'Fierce combatant! You conquered most tests, now execute the final strike! 🐅',
        'Sharp instincts! Hunt down the remaining edge cases and claim 100!',
      ],
      retry: [
        'Scratches only sharpen a tiger claws! Pounce back at that bug! 🐅💪',
        'A true warrior never yields to compile errors! Strike again with ferocity!',
      ],
      pet: [
        'Purrrrr... a fierce war tiger melting under your gentle touch! 🐅❤️',
        'White Tiger feels your respect. My claws are at your disposal!',
      ],
      idle: [
        'Prowling the codebase... waiting for bugs to ambush! 🐅',
        'Stay sharp and fearless. Every programming trial makes you stronger.',
      ],
    },
  },
  chu_tuoc: {
    dialogues: {
      perfect: [
        'The Vermilion Bird blazes in crimson glory! 100/100 perfection! 🪶🔥🎉',
        'Blazing brilliance! Your algorithms radiate with celestial fire! 🪶✨',
      ],
      good: [
        'The flame burns bright! Fan the embers to achieve a glorious 100! 🪶🔥',
        'Warm and vibrant OOP design! Push through to the highest summit!',
      ],
      retry: [
        'Reborn from the ashes! No exception can extinguish your burning spirit! 🪶🔥',
        'Burn away the erroneous syntax! Rise stronger with each revision!',
      ],
      pet: [
        'Warm sacred feathers caressing your fingers! Thank you, friend! 🪶❤️',
        'The Vermilion Bird grants you warmth, speed, and boundless inspiration!',
      ],
      idle: [
        'Wings of flame dancing gracefully beside your terminal! 🪶',
        'Let the fire of passion fuel your problem-solving journey.',
      ],
    },
  },
  huyen_vu: {
    dialogues: {
      perfect: [
        'The Black Tortoise stands unconquered! Impenetrable 100/100 defense! 🐢🛡️🎉',
        'Monumental stability! Your architecture is as steadfast as Mount Tai! 🐢✨',
      ],
      good: [
        'A fortress of solid logic! Just a few minor fortifications to reach 100! 🐢',
        'Deep and tranquil focus. You are steadily approaching supreme mastery!',
      ],
      retry: [
        'Shields held firm! Endure the storm, inspect the invariants, and rebuild! 🐢🛡️',
        'Patience of the ancient tortoise: analyze calmly and overcome the bug!',
      ],
      pet: [
        'The ancient shell resonates with deep, soothing harmony. Thank you! 🐢❤️',
        'Huyền Vũ extends calm composure and impenetrable focus to your mind.',
      ],
      idle: [
        'Guarding the northern gate of your software architecture! 🐢',
        'Deep roots withstand the fiercest winds. Write solid, defensive code.',
      ],
    },
  },
  linh_loc: {
    dialogues: {
      perfect: [
        'Linh Lộc bestows a blessing! A hundred blossoms open — absolute 100/100 enlightenment! 🦌✨🌸',
        'An extraordinary immortal root! Your algorithms shine clear as celestial jade! 🦌💎',
      ],
      good: [
        'Immortal aura drifts around you! One more step of insight and you reach 100! 🦌✨',
        'Jade light guides the path — polish those final edge cases with patience!',
      ],
      retry: [
        'Cultivation always meets a heavenly trial! Calm your mind and shatter every worldly bug! 🦌🧘‍♂️',
        'Auspicious clouds protect you — Linh Lộc grants a wisdom elixir for that compile error!',
      ],
      pet: [
        'Golden antlers brush your hand as jade bells chime a fairy melody! 🦌❤️',
        'Linh Lộc senses your pure heart. Immortal fortune favors you!',
      ],
      idle: [
        'Fairy silk and five-colored clouds swirl around your keyboard... 🦌☁️',
        'A mind as still as water makes code flow smoothly — all things complete.',
      ],
    },
  },
  ki_lan: {
    dialogues: {
      perfect: [
        'Kì Lân appears! A radiant 100/100 omen lights the three realms! 🦄✨🎉',
        'Divine might shakes all beings! Your source code has reached perfect mastery! 🦄🔥',
      ],
      good: [
        'Auspicious omens overflow! The golden horn lights the road to a perfect 100! 🦄✨',
        'Noble spirit! Deliver the finishing strike and claim peak mastery!',
      ],
      retry: [
        'This sacred beast was forged through thunder trials! Rise, revise, and break through! 🦄⚡',
        'Holy fire banishes evil! Burn every Exception and hidden bug!',
      ],
      pet: [
        'Dragon whiskers curl as a cloud mane warms your palm... 🦄❤️',
        'Kì Lân grants you fortune and endless luck on your coding path!',
      ],
      idle: [
        'Treading the clouds, guarding your project from wicked bugs! 🦄☁️',
        'Those with great resolve achieve great things. Keep advancing!',
      ],
    },
  },
  cung_ky: {
    dialogues: {
      perfect: [
        'ROAAAR! 100/100! Your code obliterates every single testcase! 😈🔥',
        'Terrifying brilliance! Even ancient perils bow to your flawless logic! 🐾⚡',
      ],
      good: [
        'GRRR! A menacing score! Sharpen your fangs and claim that 100! 😈',
        'Ferocious execution! Crush the last remaining bug without mercy!',
      ],
      retry: [
        'HMPH! A mere runtime exception dares challenge us?! Annihilate it! 🔥',
        'Unleash your inner beast! Debug ruthlessly and tear the error apart! 🐾',
      ],
      pet: [
        '...Grrr! Watch your hand mortal... Hmph, that actually feels not bad. 😈🔥',
        'Cùng Kỳ snarls softly... You dare pet an ancient peril? Bold human!',
      ],
      idle: [
        'The winged crimson beast is watching your every keystroke... 😈',
        'Show no weakness in your unit tests. Code with ferocious ambition!',
      ],
    },
  },
  con_bang: {
    dialogues: {
      perfect: [
        'The celestial leviathan breaches the cosmic seas! 100/100 transcendence! 🐋🌌🎉',
        'Starlight illuminates your architecture! Your algorithms traverse the infinite cosmos! ⭐✨',
      ],
      good: [
        'The celestial tide is rising! Channel the astral currents to reach 100! 🐋✨',
        'A magnificent flight through the stratosphere! Polish the last boundary condition!',
      ],
      retry: [
        'Deep ocean depths forge immortal wings! Rise above the error and soar higher! 🐋🌊',
        'Even the vast Kun Peng dives deep before launching into the nine heavens! Debug calmly!',
      ],
      pet: [
        'Gentle ripples of cosmic stardust warm your hands... Thank you, stargazer! 🐋❤️',
        'Côn Bằng blesses your journey with boundless vision and infinite depth!',
      ],
      idle: [
        'Gliding gracefully across the boundless sea of code and data... 🐋',
        'Broaden your horizons. Think beyond the ordinary and build monumental systems.',
      ],
    },
  },
  shiba: {
    dialogues: {
      perfect: [
        'Much 100/100! Very clean code! Such legendary OOP architecture! 🐕🎉',
        'Wagging tail at maximum velocity! Flawless submission, hooman! ⭐🐾',
      ],
      good: [
        'Much wow! Impressive score! Just one tiny treat away from 100! 🐕✨',
        'Sniffing out that remaining edge case! You are doing amazing!',
      ],
      retry: [
        'Bork! Do not be sad! Shiba digs up the hidden bug for you! 🐕🔍',
        'Shake the dust off! Take a walk, grab a snack, and run tests again!',
      ],
      pet: [
        'Aww yiss, good boy scratches! Shiba loves you so much! 🐕❤️',
        'Tail wags happily! Ready to code and guard your IDE all day!',
      ],
      idle: [
        'Sniffing around your repository... All functions looking healthy! 🐕',
        'Always loyal, always watching your compile output with pride!',
      ],
    },
  },
  zerotwo: {
    dialogues: {
      perfect: [
        'Darling, 100/100! You truly are my special partner! 🌸✨',
        'Magnificent execution! Together, no compiler in the world can stop us! 🌺⚡',
      ],
      good: [
        'Not bad at all, darling! But I know you have the strength to reach 100! 🌸',
        'A thrilling flight! Let us push the engine a little harder next time!',
      ],
      retry: [
        "Do not look down, darling. Ride with me and let's tear through that bug! 💪🌸",
        'A minor setback means nothing to us. Stand tall and refactor!',
      ],
      pet: [
        'Hehe... You are so gentle, darling. I like when you touch my horns. 🌸❤️',
        'My heart races when you code with such passion. Keep going, darling!',
      ],
      idle: [
        'Are you ready to fly with me, darling? Keep your focus sharp! 🌸',
        'I will always be by your side, through every commit and test suite.',
      ],
    },
  },
  ice_queen: {
    dialogues: {
      perfect: [
        'A flawless 100. As expected of my partner. Immaculate execution. ❄️👑',
        'Absolute perfection. Not even a single edge case dared to slip through.',
      ],
      good: [
        'Commendable effort. But I know your true potential reaches far beyond this.',
        'A strong score, yet perfection awaits. Do not settle so easily.',
      ],
      retry: [
        'A minor chill. Compose your mind, inspect the trace, and freeze that flaw.',
        'Failure is merely unrefined logic. Rise and resolve it.',
      ],
      pet: [
        '...Do not think a gentle touch will melt my standards. Though... I accept it.',
        'A fleeting warmth. Return to your code now.',
      ],
      idle: [
        'Precision and discipline. The realm of code honors only absolute focus. ❄️',
        'I am watching. Let us see what elegance you craft today.',
      ],
    },
  },
  capybara: {
    dialogues: {
      perfect: [
        'Zero stress, 100/100! Soaking in the hot spring to celebrate your clean code!',
        'Unbothered, hydrated, and passing 100% of testcases effortlessly!',
      ],
      good: [
        'Super chill progress! Balance a tangerine on your head and grab those last points!',
        'Calm mind, clean OOP structure! Almost at 100!',
      ],
      retry: [
        'No panic! Take a deep capybara breath and trace that method calmly.',
        'Bugs come and go, inner peace stays forever. Fix it gently and resubmit!',
      ],
      pet: [
        'Purr... warm head pats and sakura petals, life is wonderful!',
        'Capybara shares tranquil anti-bug energy with you!',
      ],
      idle: [
        'Floating peacefully while keeping a cozy eye on your classes.',
        'Stay calm and code steady—every testcase yields to patience.',
      ],
    },
  },
  alpaca: {
    dialogues: {
      perfect: [
        'Fluffy cloud perfection! 100/100 sparkling brighter than cotton candy!',
        'Bouncing on pastel clouds for your spotless OOP solution!',
      ],
      good: [
        'So soft, so clean! Just one tiny polish left to hit 100!',
        'Wonderful score! Your class design is as neat as freshly brushed fleece!',
      ],
      retry: [
        'Do not fret! Wrap up in warm wool, spot the edge case, and try again!',
        'Every great coder untangles a knot or two—you have got this!',
      ],
      pet: [
        'Poof! So fluffy! Thank you for the gentle pats!',
        'Happy alpaca hums! Cheering for your next green build!',
      ],
      idle: [
        'Drifting softly beside your workspace while you craft clean Java.',
        'Keep your methods tidy and cozy!',
      ],
    },
  },
  hai_cau: {
    dialogues: {
      perfect: [
        'Arf arf! 100/100! Time for a celebratory pearl milk tea toast!',
        ' clapping flippers nonstop! Every single testcase passed without a splash!',
      ],
      good: [
        'Smooth ocean glide! One more sip of boba and 100 is yours!',
        'Awesome OOP flow! Those edge cases stand no chance!',
      ],
      retry: [
        'Dive back under the waves, spot that sneaky bug, and resurface stronger!',
        'A quick milk tea recharge and you will ace the next upload!',
      ],
      pet: [
        'Squishy seal belly taps! Thank you so much!',
        'Bouncing happily like a boba pearl!',
      ],
      idle: [
        'Sipping pearl milk tea while watching your code compile.',
        'Ready to clap flippers for your next submission!',
      ],
    },
  },
  red_panda: {
    dialogues: {
      perfect: [
        'Hands up in total celebration! 100/100 strawberry-sweet victory!',
        'Fluffy striped tail wagging at max speed—flawless OOP mastery!',
      ],
      good: [
        'Super sharp work! Just one small berry-sized tweak to reach 100!',
        'Climbing high up the leaderboard branch! Keep going!',
      ],
      retry: [
        'Stand tall with paws up—scare that bug right out of your code!',
        'Check constructor and method signatures carefully, then strike back!',
      ],
      pet: [
        'Eee! Ear scratches make my fluffy tail wag nonstop!',
        'Red Panda is 100% energized to debug alongside you!',
      ],
      idle: [
        'Curled up near your editor, watching over your class hierarchy.',
        'Clean encapsulation makes Red Panda very happy!',
      ],
    },
  },
  pomeranian: {
    dialogues: {
      perfect: [
        ' Bork bork hooray! A 5-star gourmet 100/100 masterpiece!',
        'Spinning in happy circles—every single check turned bright green!',
      ],
      good: [
        'Whipped-cream smooth logic! Just a tiny sprinkle left for 100!',
        'Such proud puppy energy! Your OOP skills are shining!',
      ],
      retry: [
        'Paws on the keyboard—we will sniff out that bug in seconds!',
        'Shake off the compile dust and give it another confident try!',
      ],
      pet: [
        'Tail wagging like a turbo fan! Thank you for the head pats!',
        'Loyal Pom-Pom is cheering for every line you write!',
      ],
      idle: [
        'Perky ears listening for a clean green build!',
        'Ready to celebrate your next lab milestone!',
      ],
    },
  },
  ho_diep: {
    dialogues: {
      perfect: [
        'Crystal prism wings in full bloom! A radiant 100/100 metamorphosis!',
        'Every testcase refracted into pure emerald perfection!',
      ],
      good: [
        'Graceful architecture! One final wingbeat will carry you to 100!',
        'Luminous OOP structure—nearly flawless!',
      ],
      retry: [
        'Even the brightest butterfly refines its chrysalis. Polish that logic and soar!',
        'Let starlight illuminate the hidden boundary condition.',
      ],
      pet: [
        'Iridescent wing-dust shimmers softly at your touch!',
        'May crystal clarity guide your thoughts and algorithms.',
      ],
      idle: [
        'Fluttering serenely amidst prism light while you design your classes.',
        'Harmony between diagram and code creates true elegance.',
      ],
    },
  },
  thien_nga: {
    dialogues: {
      perfect: [
        'A grand Swan Lake finale—100/100 with crystalline poise and precision!',
        'Moonlit crown gleaming! Not a single flaw across the entire suite!',
      ],
      good: [
        'An elegant performance! One final pirouette to claim a perfect 100!',
        'Refined inheritance and clean methods—truly graceful!',
      ],
      retry: [
        'Keep your posture poised; review the failing step and glide past it.',
        'Calm waters reflect the sharpest truth. Inspect your edge cases gently.',
      ],
      pet: [
        'Soft moonlit feathers rustle in quiet gratitude.',
        'A graceful bow—let us compose impeccable code together.',
      ],
      idle: [
        'Gliding across starlit waters, guarding your focus.',
        'True elegance in OOP comes from clarity and balance.',
      ],
    },
  },
  cuu_vi: {
    dialogues: {
      perfect: [
        'Nine sakura tails blaze in celestial triumph—an immaculate 100/100!',
        'Even ancient immortals marvel at your flawless OOP dominion!',
      ],
      good: [
        'Clever spirit! Just a whisper of fox-fire away from a perfect 100!',
        'Sharp wit and clean abstractions—press onward to perfection!',
      ],
      retry: [
        'See past the illusion of that bug; trace the true state and strike again!',
        'Nine tails, ninefold resilience—no edge case can outwit us!',
      ],
      pet: [
        'Hmph... Very well, you may brush my fluffy sakura tails just a little longer.',
        'Warm spirit-lantern light blesses your coding session!',
      ],
      idle: [
        'Nine tails swaying gently as sakura petals drift past your code.',
        'Craft your classes with cunning precision.',
      ],
    },
  },
  stella_witch: {
    dialogues: {
      perfect: [
        'The Astral Tarot foretold it—a spellbinding 100/100 across the cosmos!',
        'Grand Grimoire unlocked! Every single testcase aligned like constellations!',
      ],
      good: [
        'Potent spellcraft! Stir one last pinch of starlight into your edge cases!',
        'The stars shine favorably upon your OOP design!',
      ],
      retry: [
        'When a potion bubbles over, adjust the formula! Check your null and boundary guards!',
        'My familiar spotted a tiny hex in the logic—let us dispel it!',
      ],
      pet: [
        'Careful not to tilt my starlight witch hat! Hehe, thank you!',
        'Casting a +100% Focus blessing upon your keyboard!',
      ],
      idle: [
        'Reading star charts and class diagrams beside you.',
        'Every clean method is a well-written incantation.',
      ],
    },
  },
  sakura_princess: {
    dialogues: {
      perfect: [
        'Full Moon Blossom Ascension! A royal 100/100 worthy of the Lunar Palace!',
        'The koto strings resonate in celebration of your flawless architecture!',
      ],
      good: [
        'Exquisite craftsmanship! Just one delicate brushstroke left for 100!',
        'Serene and noble logic—perfection is within reach!',
      ],
      retry: [
        'Share a cup of warm matcha, calm your heart, and refine the failing method.',
        'Like blossoms after frost, your next attempt will bloom even brighter.',
      ],
      pet: [
        'A gentle royal smile—thank you for your kindness, dear scholar.',
        'The little jade moon-bunny hops happily for your success!',
      ],
      idle: [
        'Moonlight and cherry blossoms watch peacefully over your study.',
        'Patience and clarity turn complex requirements into art.',
      ],
    },
  },
  floral_fairy: {
    dialogues: {
      perfect: [
        'World Tree Sanctuary in full 3D bloom! A magical 100/100 triumph!',
        'Lily-of-the-valley bells ring across the screen for your perfect score!',
      ],
      good: [
        'Sparkling emerald wings! Just one tiny petal left to reach 100!',
        'Your OOP garden is thriving beautifully—keep going!',
      ],
      retry: [
        'Sprinkle a little fairy dew on that tricky testcase and watch it bloom!',
        'Do not pout! Let us fly over that class and fix it together!',
      ],
      pet: [
        'Hehe, fairy dust sparkles everywhere when you boop me!',
        'Fluttering happily around your screen to cheer you on!',
      ],
      idle: [
        'Gliding through the air in 360°, watching every class flourish!',
        'Need quiet focus? You can pin me to the Header anytime!',
      ],
    },
  },
  chrono_celestia: {
    dialogues: {
      perfect: [
        'Across every timeline in the cosmos, your 100/100 shines like a supernova!',
        'The golden armillary sphere aligns—absolute perfection achieved!',
      ],
      good: [
        'Time flows in your favor! One final cosmic convergence to reach 100!',
        'Stellar OOP symmetry—almost at the singularity of perfection!',
      ],
      retry: [
        'Let us rewind that step, inspect the state transition, and rewrite fate!',
        'Even stars collapse before igniting—debug boldly and submit again!',
      ],
      pet: [
        'Starlight ripples gently across time at your touch.',
        'Orbiting beside you through every challenge and milestone!',
      ],
      idle: [
        'Cruising across space-time while keeping watch over your logic.',
        'Every second invested in clean design compounds across eternity.',
      ],
    },
  },
  cyber_striker: {
    dialogues: {
      perfect: [
        'Omega Quantum Seraph Mode engaged—100/100 target obliterated!',
        'Zero bugs detected on radar! Tactical execution rated SSS+!',
      ],
      good: [
        'Thrusters at 95% output! Calibrate that final edge case for full 100% sync!',
        'Solid armor-grade OOP architecture, Commander!',
      ],
      retry: [
        'Hull intact! Pull up the diagnostic HUD, patch the anomaly, and relaunch!',
        'A true pilot never quits—reroute power to debugging and fire again!',
      ],
      pet: [
        'Arc-Reactor core synced at 120% morale! Ready for deployment!',
        'Bit-drones salute you, Commander! Let us crush the next lab!',
      ],
      idle: [
        'Patrolling your viewport at 100 FPS—all systems nominal.',
        'Scanning class signatures and constructors in real time.',
      ],
    },
  },
  kiem_de: {
    dialogues: {
      perfect: [
        'Ten-Thousand Swords bow before your flawless 100/100 Sword Domain!',
        'One strike, zero bugs—your OOP mastery has reached the Immortal Realm!',
      ],
      good: [
        'Formidable sword-intent! Temper one last edge case to ascend to 100!',
        'Clean, decisive logic—worthy of an inner-court grandmaster!',
      ],
      retry: [
        'Steady your qi! Discern the flaw in the formation and cleave right through it!',
        'A true sword sovereign sharpens his blade on every trial. Strike again!',
      ],
      pet: [
        'Hah! A toast from my jade gourd to a worthy fellow cultivator!',
        'My spirit blades hum in resonance with your fighting spirit!',
      ],
      idle: [
        'Soaring on my astral broadsword across the heavens as you forge your code.',
        'When mind and specification become one, no testcase can stand against you.',
      ],
    },
  },
};

export function getMascotProfile(id) {
  if (!id) return MASCOT_PROFILES.fox;
  const raw = String(id).trim();
  const lower = raw.toLowerCase();
  const normalized = lower.replace(/-/g, '_');
  return MASCOT_PROFILES[raw] || MASCOT_PROFILES[lower] || MASCOT_PROFILES[normalized] || MASCOT_PROFILES.fox;
}

export function getRandomDialogue(mascotId, eventType) {
  const profile = getMascotProfile(mascotId);
  const own = profile.dialogues?.[eventType];
  const list = own && own.length > 0 ? own : (MASCOT_PROFILES.fox.dialogues[eventType] || []);
  if (list.length === 0) return 'Keep up the great work!';
  return list[Math.floor(Math.random() * list.length)];
}
