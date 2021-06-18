# [부스트캠프 AI Tech p4-GTS Side Project](https://github.com/bcaitech1/p4-opt-2-GTS)

![](https://user-images.githubusercontent.com/34030303/122388930-52c60b00-cfab-11eb-8b41-d5434dacdab3.png)

**마스크 체크인**은 **초 경량화 Image Classification** 모델이 탑재된 **On-Device Android App**입니다. 대형 마트, 지하철과 같은 공공 장소에서 QR 체크 뿐만 아니라 **마스크 착용 여부를 식별**하기 위해서 개발되었습니다. On-Device AI 기술을 사용하기 때문에 사용자는 **인터넷 이용 없이** 사용할 수 있고, 화면에 얼굴이 들어왔을 때 마스크 착용 여부를 음성으로 안내해줍니다.

### Pretrained 모델과 성능 비교(Galaxy Note 8 기준)

| 모델 이름              | latency     | 용량      | parameter | 양자화 |
| ---------------------- | ----------- | --------- | --------- | ------ |
| efficientnet_b0        | 약 300ms    | 5.9mb     | 5.2M      | O      |
| efficientnet_lite0     | 약 220ms    | 5mb       | 4.7M      | O      |
| mobilenet_v1_025       | 약 80ms     | 0.7mb     | 0.47M     | O      |
| mobilenet_v3_small_075 | 약 100ms    | 2.3mb     | 2.4M      | O      |
| **GTSNet**             | **약 50ms** | **0.6mb** | **0.15M** | **X**  |

### 시연 영상

![인증되었습니다](https://user-images.githubusercontent.com/34030303/122573353-ac9d0280-d089-11eb-8ca9-8b4e6842d09d.gif)

![마스크를 착용해주세요](https://user-images.githubusercontent.com/34030303/122573477-cc342b00-d089-11eb-93d8-30fb00df0104.gif)

**마스크 체크인**은 [구글 play store](https://play.google.com/store/apps/details?id=com.jihopark.mlkit.vision.demo)에서 다운 받을 수 있습니다.

