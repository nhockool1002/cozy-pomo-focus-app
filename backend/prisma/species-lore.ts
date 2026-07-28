/**
 * Câu chuyện ngắn (Species.lore) cho toàn bộ 175 loài — mỗi loài 1 câu chuyện riêng, không lặp
 * công thức. Key = tên loài (khớp chính xác forestNames/seaNames/plantNames/mythicNames trong
 * seed.ts). Tách file riêng vì seed.ts đã khá dài — chỉ import 1 map duy nhất vào đó.
 */
export const SPECIES_LORE: Record<string, string> = {
  // ---------- FOREST (50) ----------
  // fox
  'Cáo Pomodoro': 'Chiếc đồng hồ cát nhỏ quanh cổ nó chưa từng chạy sai — hễ 25 phút trôi qua là nó lại ngoe nguẩy đuôi nhắc bạn nghỉ tay.',
  'Cáo Buổi Sớm': 'Luôn là kẻ đầu tiên thức dậy trong khu rừng, nó thích ngồi trên gò đất hong bộ lông ướt sương chờ mặt trời lên.',
  'Cáo Áo Len': 'Một bà cụ đan cho nó chiếc áo len quá khổ, giờ nó chẳng chịu cởi ra dù trời có ấm đến đâu.',
  'Cáo Sương Mù': 'Nó biết mọi lối tắt trong khu rừng phủ sương dày đặc mà không con vật nào khác dám bước vào.',
  'Cáo Lá Phong': 'Mỗi mùa thu nó lại nhặt một chiếc lá phong đỏ nhất giắt sau tai, coi đó là huy hiệu của riêng mình.',
  // rabbit
  'Thỏ Mộng Mơ': 'Nó ngủ nhiều hơn bất kỳ con thỏ nào trong rừng, và luôn khăng khăng rằng giấc mơ đêm qua đẹp hơn cả thực tại.',
  'Thỏ Bông Gòn': 'Bộ lông của nó bồng lên như một cụm mây nhỏ, hễ chạy qua bụi cây là để lại vài sợi lông vướng lại.',
  'Thỏ Trà Chiều': 'Đúng 4 giờ chiều mỗi ngày, nó ngồi im trước một chiếc lá sen đọng sương, giả vờ đó là tách trà của mình.',
  'Thỏ Cỏ Ba Lá': 'Truyền thuyết trong rừng kể rằng ai tìm thấy nó đang gặm cỏ ba lá bốn cánh sẽ có một ngày thật may mắn.',
  'Thỏ Đêm Trăng': 'Chỉ ra khỏi hang khi trăng tròn, nó nhảy múa một mình giữa bãi cỏ như thể đang đếm từng ánh sao.',
  // bear
  'Gấu Ngái Ngủ': 'Nó có thể ngủ gật ngay giữa câu chuyện đang kể, và không ai trong rừng nỡ đánh thức nó dậy.',
  'Gấu Mật Ong': 'Mũi nó lúc nào cũng dính chút mật vàng óng, dấu vết của tổ ong nó vừa "mượn tạm" sáng nay.',
  'Gấu Chăn Ấm': 'Nó tha một tấm chăn vá nhiều mảnh về hang, quấn quanh mình mỗi tối như một cái kén ấm áp.',
  'Gấu Bánh Quy': 'Vụn bánh quy luôn vương trên bộ lông của nó — hình như nó chưa từng ăn hết một chiếc mà không làm rơi vài mảnh.',
  'Gấu Rừng Thông': 'Nó sống dưới gốc thông già nhất khu rừng, và mùi nhựa thông thơm nhẹ đã ngấm vào từng sợi lông.',
  // cat
  'Mèo Lười Nắng': 'Cả buổi trưa nó chỉ làm đúng một việc: nằm dài giữa vệt nắng ấm nhất trên sàn gỗ và không nhúc nhích.',
  'Mèo Đọc Sách': 'Nó thích nằm trên chồng sách cũ hơn là đọc chúng, nhưng ai cũng tin nó thông thái nhất khu rừng.',
  'Mèo Tách Trà': 'Nó có thói quen kỳ lạ là chỉ uống nước mưa đọng trong những chiếc tách sứt mẻ bị bỏ quên.',
  'Mèo Cuộn Len': 'Một cuộn len đỏ luôn lăn theo sau nó, kết quả của vô số buổi chiều nghịch ngợm không biết chán.',
  'Mèo Đêm Sao': 'Đôi mắt nó ánh lên như hai vì sao nhỏ mỗi khi đêm xuống, khiến lũ đom đóm tưởng nhầm là đồng loại.',
  // bird
  'Chim Sẻ Rộn Ràng': 'Nó hót đúng lúc mặt trời vừa nhô khỏi ngọn cây, và cả khu rừng dùng tiếng hót ấy làm đồng hồ báo thức.',
  'Chim Cổ Đỏ': 'Vệt lông đỏ trên cổ nó đậm dần mỗi mùa đông, như thể nó giữ lại chút lửa ấm cho riêng mình.',
  'Chim Vàng Anh': 'Giọng hót của nó vang xa đến mức những loài chim khác thường im lặng lắng nghe thay vì hót theo.',
  'Chim Ri Đá': 'Nó làm tổ trong khe đá thay vì trên cành cây, và tự hào vì tổ của mình không bao giờ bị gió cuốn đi.',
  'Chim Hoạ Mi Nhỏ': 'Còn quá nhỏ để bay xa, nhưng tiếng hót của nó đã đủ ngọt ngào để cả bụi cây phải xao động.',
  // hedgehog
  'Nhím Ấm Áp': 'Dù đầy gai nhọn, nó vẫn được lũ thỏ con rủ ngủ chung vì thân nhiệt của nó ấm hơn bất kỳ ai.',
  'Nhím Gối Bông': 'Nó cuộn tròn lại mỗi khi buồn ngủ, trông chẳng khác gì một chiếc gối nhỏ bị bỏ quên giữa cỏ.',
  'Nhím Lá Khô': 'Nó nguỵ trang khéo đến mức từng nằm im giữa đống lá khô cả buổi chiều mà chẳng ai phát hiện ra.',
  'Nhím Táo Đỏ': 'Trò vui thích nhất của nó là lăn qua vườn táo rụng, để quả táo chín mắc lại trên lưng đầy gai.',
  'Nhím Đêm Sương': 'Nó chỉ ra ngoài kiếm ăn lúc sương đêm còn đọng trên cỏ, để bước chân êm hơn và không đánh thức ai.',
  // squirrel
  'Sóc Tíu Tít': 'Nó nói chuyện một mình liên tục cả ngày, đến mức lũ chim trong rừng nghĩ nó đang tập hót.',
  'Sóc Hạt Dẻ': 'Kho hạt dẻ của nó giấu ở bảy nơi khác nhau, và nó chỉ nhớ chính xác vị trí của bốn trong số đó.',
  'Sóc Mùa Thu': 'Mỗi năm nó chỉ xuất hiện đúng mùa lá vàng rơi, khiến nhiều con vật tin nó chính là dấu hiệu của mùa thu.',
  'Sóc Vân Sam': 'Nó sống trên ngọn cây vân sam cao nhất, nơi có thể nhìn thấy toàn bộ khu rừng trải dài phía dưới.',
  'Sóc Áo Nâu': 'Bộ lông nâu sậm của nó hoà lẫn vào thân cây đến mức nhiều lần chính nó cũng quên mất mình đang trốn ai.',
  // raccoon
  'Gấu Mèo Tinh Nghịch': 'Không có chiếc khoá nào trong rừng mà đôi tay khéo léo của nó chưa từng mở thử một lần.',
  'Gấu Mèo Đốm Sao': 'Những đốm trắng quanh mắt nó lấp lánh dưới trăng, trông như thể nó mượn tạm vài ngôi sao đeo lên mặt.',
  'Gấu Mèo Sương Sớm': 'Nó luôn rửa sạch từng viên đá nhỏ nhặt được trong dòng suối buổi sớm trước khi cất vào túi riêng.',
  'Gấu Mèo Lá Vàng': 'Nó thu thập lá vàng rơi rồi xếp thành từng chồng gọn gàng, dù chẳng ai hiểu nó làm vậy để làm gì.',
  'Gấu Mèo Đêm Hè': 'Những đêm hè oi ả, nó lẻn ra suối tắm mát rồi trở về hang trước khi trời kịp sáng.',
  // deer
  'Hươu Thầm Lặng': 'Nó di chuyển êm đến mức chưa một chiếc lá khô nào từng kêu lên dưới bước chân của nó.',
  'Hươu Sương Sớm': 'Mỗi sớm mai nó đứng bất động giữa đồng cỏ đẫm sương, để những giọt sương long lanh đọng trên sừng.',
  'Hươu Cỏ Xanh': 'Nó chỉ ăn cỏ non mọc quanh gốc cây cổ thụ, tin rằng nơi đó cỏ ngọt hơn bất cứ đâu trong rừng.',
  'Hươu Chuông Gió': 'Chiếc chuông gỗ nhỏ ai đó treo lên sừng nó từ lâu giờ kêu leng keng theo mỗi bước đi.',
  'Hươu Nắng Sớm': 'Nó luôn đứng quay mặt về phía đông, chờ tia nắng đầu tiên chạm vào cặp sừng non của mình.',
  // owl
  'Cú Đêm Hiền': 'Giọng "khụt khịt" của nó nhẹ đến mức những chú thỏ con vẫn ngủ ngon dù nó bay ngang qua tổ.',
  'Cú Sách Cổ': 'Nó làm tổ trong một hốc cây từng là thư viện bỏ hoang, và tin rằng mình đã đọc hết mọi cuốn sách còn sót lại.',
  'Cú Trăng Non': 'Nó chỉ cất tiếng hót vào những đêm trăng khuyết, như thể đang thì thầm điều gì đó riêng với bầu trời.',
  'Cú Gió Đêm': 'Đôi cánh của nó lướt êm trong gió đêm đến mức lũ chuột đồng gọi nó là "cái bóng không tiếng động".',
  'Cú Rừng Sâu': 'Không ai trong rừng biết chính xác tổ của nó nằm ở đâu, chỉ nghe tiếng gọi vọng ra từ nơi sâu thẳm nhất.',

  // ---------- SEA (50) ----------
  // turtle
  'Rùa May Mắn': 'Người ta đồn rằng chạm vào mai của nó một lần sẽ có một điều ước nhỏ thành sự thật trong ngày hôm đó.',
  'Rùa Đá Cuội': 'Mai của nó lấm tấm những đốm xám giống hệt viên đá cuội, khiến nó thường bị nhầm là một hòn đá biết bơi.',
  'Rùa Lá Sen': 'Nó thích nằm im dưới một chiếc lá sen nổi, đến mức đôi khi ếch con nhảy nhầm lên lưng nó mà không hay biết.',
  'Rùa Ngọc Bích': 'Ánh xanh biếc trên mai nó chỉ thật sự lấp lánh khi bơi dưới ánh trăng phản chiếu mặt nước.',
  'Rùa Sóng Vỗ': 'Nó bơi cùng nhịp với những con sóng lớn, để mỗi lần sóng vỗ vào bờ nó lại trôi theo êm ái.',
  // crab
  'Cua Nắng Chiều': 'Đúng lúc hoàng hôn nhuộm cam cả bãi biển, nó mới chịu bò ra khỏi hang để dạo một vòng.',
  'Cua Bọt Biển': 'Nó thích thổi bong bóng bọt biển nhỏ rồi đuổi theo chúng dọc bờ cát cho đến khi bọt vỡ tan.',
  'Cua Vỏ Sò': 'Càng của nó luôn ôm chặt một chiếc vỏ sò rỗng, món đồ chơi mà nó chẳng bao giờ chịu buông ra.',
  'Cua San Hô': 'Nó sống ẩn mình giữa rặng san hô rực rỡ, và màu vỏ của nó dần biến đổi để hoà cùng sắc màu xung quanh.',
  'Cua Đá Ngầm': 'Nó bám chắc vào những tảng đá ngầm nơi sóng mạnh nhất, tự hào vì chưa lần nào bị cuốn trôi.',
  // snail
  'Ốc Anh Vũ': 'Đường xoắn hoàn hảo trên vỏ nó khiến những nhà thám hiểm biển từng dừng lại chỉ để ngắm nhìn thật lâu.',
  'Ốc Mộng Mơ': 'Nó bò chậm đến mức có vẻ như đang lạc trong một giấc mơ dài không muốn tỉnh dậy.',
  'Ốc Cầu Vồng': 'Dưới ánh nắng xiên, lớp xà cừ trong vỏ nó ánh lên đủ bảy sắc màu như một cầu vồng thu nhỏ.',
  'Ốc Xoáy Nước': 'Nó thích trôi theo những vòng xoáy nước nhỏ gần rạn san hô, coi đó như một điệu nhảy của riêng mình.',
  'Ốc Biếc Ngọc': 'Vỏ ốc màu xanh biếc của nó từng được ngư dân nhặt về làm khuy áo cho những chiếc áo đẹp nhất.',
  // fish
  'Cá Vàng Nhỏ': 'Nó bơi thành vòng tròn nhỏ mỗi khi vui, một thói quen khiến cả đàn cá luôn nhận ra nó từ xa.',
  'Cá San Hô': 'Sinh ra và lớn lên giữa rặng san hô, nó thuộc lòng từng ngóc ngách như thuộc lòng nhà của mình.',
  'Cá Vằn Đốm': 'Những vệt vằn và đốm trên mình nó không con nào giống con nào, như một dấu vân tay riêng dưới nước.',
  'Cá Bảy Màu': 'Vảy của nó đổi màu nhẹ theo tâm trạng, khiến những người quan sát kỹ luôn đoán được hôm nay nó vui hay buồn.',
  'Cá Ánh Bạc': 'Khi bơi ngược dòng nắng chiều, thân hình bạc óng của nó loé lên như một tia chớp nhỏ dưới mặt nước.',
  // starfish
  'Sao Biển Lấp Lánh': 'Nó bám trên đá ngay chỗ nước nông nhất, để ánh nắng chiếu xuyên qua khiến cả cơ thể lấp lánh như ngôi sao thật.',
  'Sao Biển Cam': 'Màu cam rực của nó nổi bật đến mức những đàn cá nhỏ thường lượn quanh chỉ để ngắm nhìn.',
  'Sao Biển Đêm': 'Nó chỉ di chuyển vào ban đêm, chậm rãi bò qua đáy biển tối như một ngôi sao lạc đường tìm về bầu trời.',
  'Sao Biển Hồng Phấn': 'Sắc hồng phấn dịu dàng của nó khiến nhiều thợ lặn ví nó như một cánh hoa anh đào chìm dưới nước.',
  'Sao Biển Sương': 'Vào sáng sớm khi thuỷ triều rút, nó thường mắc lại trong vũng nước nhỏ đọng như giọt sương khổng lồ.',
  // seal
  'Hải Cẩu Lười': 'Nó có thể nằm phơi nắng trên tảng đá suốt cả ngày mà không hề có ý định nhúc nhích dù chỉ một lần.',
  'Hải Cẩu Vịnh Xanh': 'Cả đời nó chưa từng rời khỏi vịnh nước xanh trong nơi nó sinh ra, dù đại dương rộng lớn vẫy gọi ngoài kia.',
  'Hải Cẩu Băng Giá': 'Bộ lông dày của nó từng giúp nó sống sót qua mùa đông lạnh nhất mà đàn hải cẩu còn nhớ tới.',
  'Hải Cẩu Nắng Chiều': 'Nó luôn chọn đúng tảng đá hứng được nắng chiều cuối cùng trước khi mặt trời lặn hẳn.',
  'Hải Cẩu Vui Tính': 'Trò chơi ưa thích của nó là tung một quả bóng rong biển lên rồi bắt lại bằng mũi, lặp đi lặp lại không chán.',
  // dolphin
  'Cá Heo Vui Vẻ': 'Tiếng huýt sáo vui vẻ của nó vang khắp vịnh mỗi khi có bạn mới đến chơi cùng đàn.',
  'Cá Heo Sóng Nhẹ': 'Nó thích lướt theo những con sóng nhỏ nhất, nhẹ nhàng đến mức gần như không tạo ra một gợn nước nào.',
  'Cá Heo Ánh Trăng': 'Vào những đêm trăng tròn, nó bơi lên gần mặt nước chỉ để ngắm ánh trăng lung linh phản chiếu.',
  'Cá Heo Bọt Sóng': 'Nó thích lượn quanh mũi thuyền để đùa giỡn với những bọt sóng trắng xoá cuộn lên phía sau.',
  'Cá Heo Mưa Rào': 'Khi những cơn mưa rào đầu mùa rơi xuống mặt biển, nó luôn là con đầu tiên nhảy lên đón những giọt nước.',
  // jellyfish
  'Sứa Đèn Lồng': 'Cơ thể trong suốt của nó phát sáng dịu nhẹ trong đêm tối, như một chiếc đèn lồng nhỏ trôi giữa đại dương.',
  'Sứa Ánh Trăng': 'Nó chỉ nổi lên gần mặt nước vào những đêm trăng sáng, khẽ đung đưa theo từng gợn sóng bạc.',
  'Sứa Pha Lê': 'Trong suốt gần như không thể nhìn thấy, nó chỉ lộ diện khi ánh nắng chiếu xuyên qua tạo thành vệt cầu vồng nhỏ.',
  'Sứa Hồng Nhạt': 'Sắc hồng nhạt phớt trên cơ thể nó khiến những thợ lặn ví nó như một cánh hoa mỏng manh trôi dạt.',
  'Sứa Đêm Sâu': 'Nó sống ở vùng nước sâu tối nhất, nơi ánh sáng duy nhất là quầng sáng mờ toả ra từ chính cơ thể nó.',
  // octopus
  'Bạch Tuộc Tò Mò': 'Bất cứ vật gì lạ rơi xuống đáy biển, nó đều bò tới sờ thử bằng cả tám xúc tu trước khi yên tâm bỏ đi.',
  'Bạch Tuộc Mực Tím': 'Khi hoảng sợ, nó phun ra một đám mực tím thay vì đen như đồng loại, khiến kẻ săn mồi cũng phải ngơ ngác.',
  'Bạch Tuộc Cầu Vồng': 'Nó có thể đổi màu da nhanh đến mức từng lướt qua cả bảy sắc cầu vồng chỉ trong một lần trốn chạy.',
  'Bạch Tuộc Đá San Hô': 'Nó nguỵ trang khéo léo giữa rặng san hô đến mức từng bị một con cá nhỏ đậu nhầm lên đầu.',
  'Bạch Tuộc Mực Đen': 'Đám mực đen nó phun ra đậm và dày đến mức từng khiến cả một vùng nước tối sầm trong giây lát.',
  // seahorse
  'Cá Ngựa Vằn': 'Những vệt vằn trên thân nó đậm nhạt khác nhau tuỳ theo vùng nước nó bơi qua, như một cuốn nhật ký sống.',
  'Cá Ngựa San Hô': 'Nó bám đuôi vào một nhánh san hô cố định, coi đó là ngôi nhà duy nhất suốt cả cuộc đời.',
  'Cá Ngựa Hoàng Hôn': 'Màu thân nó ánh cam đỏ giống hệt bầu trời lúc hoàng hôn, đẹp nhất khi bơi ngược ánh nắng cuối ngày.',
  'Cá Ngựa Lá Rong': 'Những tua rong biển mọc quanh thân nó khiến kẻ thù luôn nhầm nó với một nhánh rong trôi vô hại.',
  'Cá Ngựa Sương Mai': 'Vào sáng sớm khi nước còn lành lạnh, nó bơi chậm rãi qua thảm rong như đang dạo bước giữa sương mai.',

  // ---------- PLANT (50) ----------
  // flowerRound
  'Hoa Tulip': 'Nó chỉ nở trọn vẹn vào đúng buổi sáng có nắng ấm đầu tiên sau một đêm mưa dài.',
  'Hoa Cúc Nắng': 'Cánh hoa vàng của nó luôn xoay theo hướng mặt trời, như đang dõi theo một người bạn cũ suốt cả ngày.',
  'Hoa Anh Đào Nhỏ': 'Chỉ nở đúng ba ngày mỗi năm, nhưng cả khu vườn đều nhớ lịch nở hoa của nó không sai một lần.',
  'Hoa Mẫu Đơn': 'Cánh hoa dày và rực rỡ của nó từng được ví như một chiếc váy dạ hội bung nở giữa khu vườn.',
  'Hoa Oải Hương Mini': 'Mùi hương dịu nhẹ của nó lan xa đến mức ong bướm thường bay lạc đến từ những khu vườn khác.',
  // flowerStar
  'Hoa Sao Biếc': 'Năm cánh hoa xanh biếc xoè đều như một ngôi sao nhỏ rơi xuống nằm nghỉ giữa thảm cỏ.',
  'Hoa Dạ Yến Sao': 'Nó chỉ toả hương thơm ngát vào ban đêm, khi những vì sao thật sự bắt đầu xuất hiện trên bầu trời.',
  'Hoa Cẩm Tú Sao': 'Cả một khóm hoa của nó nở cùng lúc trông như một chòm sao nhỏ rơi lạc xuống khu vườn.',
  'Hoa Bìm Bìm Sao': 'Nó leo theo hàng rào gỗ cũ, mỗi sáng lại nở thêm vài bông hình sao mới tinh khôi.',
  'Hoa Thảo Nguyên Sao': 'Mọc rải rác khắp thảo nguyên, từ xa trông chúng như một mảng trời sao rơi xuống mặt đất.',
  // mushroom
  'Nấm Chấm Bi': 'Những chấm trắng trên mũ nấm đỏ của nó khiến trẻ con trong làng tin rằng đây là nhà của các nàng tiên nhỏ.',
  'Nấm Rêu Phong': 'Nó chỉ mọc trên những gốc cây phủ đầy rêu xanh, nơi ẩm ướt và yên tĩnh nhất khu rừng.',
  'Nấm Đèn Lồng': 'Vào những đêm không trăng, mũ nấm của nó phát ra ánh sáng xanh dịu như một chiếc đèn lồng nhỏ giữa rừng.',
  'Nấm San Hô': 'Hình dáng phân nhánh của nó trông giống hệt một nhánh san hô lạc trôi lên sống trên cạn.',
  'Nấm Cổ Tích': 'Người ta kể rằng ai tìm thấy một vòng tròn nấm này mọc qua đêm sẽ gặp một điều kỳ diệu nhỏ trong ngày.',
  // fern
  'Dương Xỉ Xoăn': 'Những chiếc lá non của nó cuộn tròn như một dấu hỏi xanh trước khi từ từ bung thẳng ra dưới nắng.',
  'Dương Xỉ Sương Mai': 'Mỗi sáng sớm, hàng trăm giọt sương nhỏ đọng đều tăm tắp trên từng phiến lá xẻ của nó.',
  'Dương Xỉ Lá Kim': 'Những chiếc lá mảnh như kim của nó khẽ rung lên mỗi khi có cơn gió nhẹ nhất lướt qua khu vườn.',
  'Dương Xỉ Rừng Sâu': 'Nó chỉ sống được dưới tán rừng rậm rạp, nơi ánh sáng mặt trời chỉ lọt qua vài khe hở nhỏ.',
  'Dương Xỉ Ngọc Bích': 'Sắc xanh đậm gần như trong suốt của lá nó khiến người ta liên tưởng đến những viên ngọc bích quý giá.',
  // succulent
  'Sen Đá Hồng': 'Đầu lá của nó ửng hồng nhẹ mỗi khi nắng gắt, như thể đang e thẹn trước ánh mặt trời.',
  'Sen Đá Ngọc': 'Những chiếc lá tròn mọng nước của nó trong suốt và bóng bẩy như được tạc từ đá ngọc thật.',
  'Sen Đá Mật Ong': 'Sắc vàng nâu ấm áp của nó gợi nhớ đến giọt mật ong sánh đặc dưới ánh nắng chiều.',
  'Sen Đá Tuyết': 'Lớp phấn trắng mịn phủ trên lá khiến nó trông như vừa được rắc một lớp tuyết mỏng.',
  'Sen Đá Rêu': 'Nó mọc thành từng cụm nhỏ li ti, xanh mướt và dày đặc như một tấm thảm rêu thu nhỏ.',
  // cactus
  'Xương Rồng Tí Hon': 'Dù bé nhỏ nhất trong họ xương rồng, nó vẫn kiên cường sống sót qua những đợt nắng hạn dài nhất.',
  'Xương Rồng Sa Mạc': 'Thân hình vạm vỡ của nó từng trải qua không biết bao mùa cát nóng trước khi được mang về khu vườn.',
  'Xương Rồng Hoa Nở': 'Cả năm nó chỉ nở đúng một bông hoa duy nhất, nhưng bông hoa ấy luôn khiến cả khu vườn phải trầm trồ.',
  'Xương Rồng Chấm Sao': 'Những chấm gai trắng đều đặn trên thân nó lấp lánh dưới nắng như một bầu trời sao thu nhỏ.',
  'Xương Rồng Mini Chum': 'Mọc thành từng chum tròn nhỏ xíu, nó thường được đặt trên bậu cửa sổ như một vật trang trí đáng yêu.',
  // berry
  'Bụi Dâu Rừng': 'Từng chùm quả đỏ mọng của nó luôn là món quà đầu tiên báo hiệu mùa hè đã chính thức bắt đầu.',
  'Bụi Việt Quất': 'Chim chóc trong vườn thường tụ tập quanh nó nhiều nhất mỗi khi quả chín rộ vào cuối hè.',
  'Bụi Phúc Bồn Tử': 'Vị chua ngọt đặc trưng của quả nó khiến nó trở thành bụi cây được hái trộm nhiều nhất khu vườn.',
  'Bụi Dâu Tằm': 'Ngày xưa lá của nó từng nuôi sống cả một đàn tằm, còn quả chín thì nhuộm tím cả bàn tay ai chạm vào.',
  'Bụi Quả Chuông': 'Những quả nhỏ hình chuông của nó khẽ đung đưa và va vào nhau tạo ra âm thanh lách cách vui tai.',
  // bamboo
  'Trúc Cảnh Nhỏ': 'Dù chỉ cao bằng một gang tay, nó vẫn vươn thẳng đầy kiêu hãnh như những cây trúc cổ thụ trong rừng.',
  'Trúc Sương Sớm': 'Mỗi sớm mai, những đốt trúc xanh của nó đọng đầy sương như được khoác một lớp áo bạc mỏng.',
  'Trúc Ngọc Xanh': 'Thân trúc của nó xanh bóng và trơn nhẵn đến mức ánh nắng chiếu vào cũng phản chiếu lấp lánh như ngọc.',
  'Trúc Vàng Chiều': 'Vào lúc hoàng hôn, thân trúc của nó ánh lên sắc vàng ấm áp hoà cùng ánh nắng cuối ngày.',
  'Trúc Mini Ban Công': 'Nó được trồng trong một chiếc chậu nhỏ trên ban công, nhưng vẫn vươn cao đón từng cơn gió sớm.',
  // vine
  'Dây Trầu Bà': 'Những chiếc lá hình trái tim của nó leo phủ kín cả một góc tường cũ chỉ sau vài mùa mưa.',
  'Dây Thường Xuân': 'Nó bám chắc vào bất cứ nơi nào có thể, và không ai từng thấy nó ngừng vươn dài dù chỉ một mùa.',
  'Dây Hoa Chuông': 'Từng chùm hoa hình chuông nhỏ của nó rung khẽ theo gió, như đang ngân nga một bản nhạc thầm lặng.',
  'Dây Lan Tim': 'Những chiếc lá hình trái tim của nó thường được người ta hái tặng nhau như một lời chúc dịu dàng.',
  'Dây Bìm Bìm Leo': 'Chỉ trong một đêm, nó có thể leo cao thêm cả gang tay để kịp nở hoa đón bình minh.',
  // tree
  'Cây Sồi Con': 'Dù còn nhỏ, nó đã bắt đầu mơ về ngày trở thành một cây sồi cổ thụ toả bóng mát cho cả khu vườn.',
  'Cây Phong Nhỏ': 'Mỗi mùa thu, những chiếc lá đầu tiên chuyển sang màu đỏ cam luôn là lá của cây phong nhỏ này.',
  'Cây Táo Tí Hon': 'Nó chỉ ra được vài quả táo mỗi năm, nhưng quả nào cũng ngọt đến mức không ai nỡ để dành lâu.',
  'Cây Liễu Xanh': 'Những cành liễu mềm mại của nó rủ xuống mặt hồ, khẽ đung đưa theo từng làn gió thoảng qua.',
  'Cây Thông Mini': 'Được trồng trong chậu nhỏ trên bàn học, nó vẫn toả ra mùi nhựa thông thơm the mát mỗi khi trời se lạnh.',

  // ---------- MYTHIC (25) ----------
  // phoenix
  'Phượng Hoàng Bình Minh': 'Nó chỉ xuất hiện đúng khoảnh khắc mặt trời nhô lên khỏi đường chân trời, đôi cánh rực lửa như đang mang bình minh đến trần gian.',
  'Phượng Hoàng Lửa Ấm': 'Ngọn lửa quanh mình nó chưa từng thiêu cháy một cọng cỏ nào, chỉ toả ra hơi ấm dịu dàng cho vạn vật xung quanh.',
  'Phượng Hoàng Chiều Tà': 'Đôi cánh của nó nhuộm màu cam đỏ giống hệt bầu trời lúc hoàng hôn, và nó chỉ bay lượn vào đúng khoảnh khắc ấy mỗi ngày.',
  'Phượng Hoàng Tro Tàn Tái Sinh': 'Truyền thuyết kể rằng mỗi trăm năm nó hoá thành tro bụi rồi hồi sinh mạnh mẽ hơn từ chính đống tro ấy.',
  'Phượng Hoàng Ánh Kim': 'Lông vũ của nó ánh lên sắc vàng kim quý giá, và người xưa tin rằng nhìn thấy nó bay qua là điềm báo của thịnh vượng.',
  // qilin
  'Kỳ Lân Ngọc Bích': 'Chiếc sừng duy nhất trên đầu nó trong suốt như ngọc bích, và nó chỉ xuất hiện trước những tâm hồn thuần khiết nhất.',
  'Kỳ Lân Mây Trắng': 'Mỗi bước chân của nó để lại một vệt mây trắng mỏng, như thể nó đang bước đi giữa tầng không thay vì mặt đất.',
  'Kỳ Lân Sao Đêm': 'Bộ lông của nó lấp lánh những đốm sáng nhỏ như sao trời, chỉ hiện rõ khi màn đêm buông xuống thật sâu.',
  'Kỳ Lân Rừng Cổ': 'Nó sống ẩn mình trong khu rừng nguyên sinh cổ xưa nhất, nơi ít ai từng đặt chân đến trong nhiều thế kỷ.',
  'Kỳ Lân Ánh Bình Minh': 'Vào khoảnh khắc đầu tiên mặt trời mọc, sừng của nó phản chiếu ánh sáng thành một cầu vồng nhỏ giữa sương sớm.',
  // dragon
  'Long Vân Thanh': 'Nó cưỡi trên những đám mây xanh biếc, và mỗi lần nó bay qua, bầu trời phía dưới lại đổ một cơn mưa nhẹ lành.',
  'Hoả Long Nhỏ': 'Dù nhỏ tuổi nhất trong họ rồng lửa, hơi thở của nó đã đủ sức thắp sáng cả một vùng núi tối vào ban đêm.',
  'Băng Long Tuyết': 'Hơi thở của nó đóng băng cả mặt hồ trong tích tắc, nhưng lớp băng ấy lại lấp lánh đẹp đến nao lòng.',
  'Thổ Long Cổ Mộc': 'Thân hình nó phủ đầy rêu và vỏ cây cổ thụ, đến mức nhiều người từng nhầm nó là một ngọn đồi đang say ngủ.',
  'Kim Long Ánh Nắng': 'Vảy vàng óng của nó phản chiếu ánh mặt trời chói loà đến mức không ai dám nhìn thẳng khi nó bay ngang bầu trời.',
  // ninetail
  'Cửu Vĩ Hồ Ly': 'Chín chiếc đuôi của nó mỗi cái mang một sức mạnh riêng, và người ta tin chỉ những ai thật lòng mới thấy được cả chín đuôi cùng lúc.',
  'Hồ Ly Sương Trắng': 'Nó di chuyển êm như sương sớm, để lại một làn hơi trắng mỏng tan biến ngay khi có ai định chạm vào.',
  'Hồ Ly Lửa Tím': 'Ngọn lửa tím kỳ lạ bao quanh đuôi nó không hề nóng, mà toả ra một cảm giác yên bình lạ thường.',
  'Hồ Ly Trăng Bạc': 'Bộ lông của nó chỉ ánh lên sắc bạc lung linh dưới ánh trăng rằm, những đêm khác nó gần như vô hình.',
  'Hồ Ly Chín Đuôi Vàng': 'Truyền thuyết cổ xưa nhất kể về nó như vị thần hộ mệnh của những khu rừng thiêng, nơi muôn loài đều được che chở.',
  // crane
  'Tiên Hạc Ngàn Năm': 'Đã sống qua ngàn năm, nó mang trong mình ký ức của biết bao mùa hoa nở rồi tàn mà không một ai khác còn nhớ.',
  'Hạc Trắng Vân Du': 'Nó không bao giờ dừng lại quá một đêm ở cùng một nơi, mải miết rong ruổi khắp chín tầng mây.',
  'Hạc Đỏ Bình Minh': 'Đôi cánh đỏ rực của nó luôn tung bay đúng lúc bình minh ló rạng, như đang chào đón một ngày mới bắt đầu.',
  'Hạc Ngọc Sương Mai': 'Lông vũ của nó điểm những giọt sương mai kết tinh thành ngọc, lấp lánh mỗi khi nó vươn cánh bay lên.',
  'Hạc Thần Gió Nam': 'Mỗi lần vỗ cánh, nó gọi về một cơn gió nam ấm áp, xua tan cái lạnh cuối đông cho cả vùng đất phía dưới.',
  // T-127 — loài thứ 176, riêng biệt ngoài 25 Thần Thú theo họ ở trên (không thuộc nhóm nào trong
  // 5 nhóm mythicByFamily) — chỉ nở ra từ Trứng Kapi do Admin phát, không rơi ngẫu nhiên ở trứng nào khác.
  'Kapi Ngái Ngủ': 'To lớn hơn hẳn muôn thú trong rừng, nó chỉ tỉnh giấc đúng 2 lần mỗi ngày — một lần để ăn, một lần để tìm chỗ êm hơn rồi ngủ tiếp. Không ai từng thấy nó thức trọn một buổi chiều.',
};
