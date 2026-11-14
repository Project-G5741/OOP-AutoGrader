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
        'Huyen Vu extends calm composure and impenetrable focus to your mind.',
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
        'Linh Lộc ban phúc! Bách hoa khai mở, đắc đạo 100/100 tuyệt đối! 🦌✨🌸',
        'Tiên căn bất phàm! Thuật toán của bạn trong trẻo như ngọc bích thiên hà! 🦌💎',
      ],
      good: [
        'Tiên khí phiêu dạt! Chỉ một bước ngộ đạo nữa thôi là chạm đỉnh 100! 🦌✨',
        'Ánh ngọc lấp lánh soi đường, kiên trì hoàn thiện những điều kiện biên cuối cùng!',
      ],
      retry: [
        'Tu tiên ắt gặp thiên kiếp! Tĩnh tâm đả tọa, phá tan mọi bug trần thế! 🦌🧘‍♂️',
        'Tường vân che chở, Linh Lộc trao bạn linh đan trí tuệ để vượt qua compile error!',
      ],
      pet: [
        'Gạc vàng khẽ chạm vào tay bạn, chuông ngọc reo vang khúc nhạc thần tiên! 🦌❤️',
        'Linh Lộc cảm nhận được tâm hồn thanh khiết của bạn. Tiên vận hanh thông!',
      ],
      idle: [
        'Dải lụa tiên và mây ngũ sắc đang lượn quanh bàn phím của bạn... 🦌☁️',
        'Tâm tĩnh như nước thì code tự khắc hanh thông, vạn sự viên mãn.',
      ],
    },
  },
  ki_lan: {
    dialogues: {
      perfect: [
        'Kỳ Lân xuất thế! Điềm lành 100/100 rực rỡ chiếu sáng toàn tam giới! 🦄✨🎉',
        'Thần uy chấn nhiếp muôn loài! Mã nguồn của bạn đạt cảnh giới viên mãn! 🦄🔥',
      ],
      good: [
        'Tường thụy ngập tràn! Độc giác hoàng kim đang soi sáng đường tới điểm 100! 🦄✨',
        'Khí phách hiên ngang! Hãy tung đòn dứt điểm để đạt tuyệt đỉnh công phu!',
      ],
      retry: [
        'Thần thú tôi luyện qua lôi kiếp! Đứng dậy chỉnh sửa và đột phá cảnh giới! 🦄⚡',
        'Lửa thiêng trừ tà! Đốt cháy mọi Exception và bug ẩn nấp!',
      ],
      pet: [
        'Râu rồng uốn lượn, bờm mây áp vào lòng bàn tay ấm áp... 🦄❤️',
        'Kỳ Lân trao cho bạn phúc khí và sự may mắn vô tận trên con đường lập trình!',
      ],
      idle: [
        'Đạp mây canh giữ dự án, xua tan mọi tà khí và bug hiểm độc! 🦄☁️',
        'Người có chí lớn tất thành đại sự. Cứ vững bước tiến lên!',
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
};

export function getMascotProfile(id) {
  return MASCOT_PROFILES[id] || MASCOT_PROFILES.fox;
}

export function getRandomDialogue(mascotId, eventType) {
  const profile = getMascotProfile(mascotId);
  const list = profile.dialogues?.[eventType] || MASCOT_PROFILES.fox.dialogues[eventType] || [];
  if (list.length === 0) return 'Keep up the great work!';
  return list[Math.floor(Math.random() * list.length)];
}
