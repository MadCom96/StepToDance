import Styles from "../../styles/guide/Card.module.css";
import { useState } from "react";
import { useSpring, animated } from "react-spring";
import { useNavigate } from "react-router-dom";

function Card({ item }) {
  const [show, setShown] = useState(false);
  const navigate = useNavigate();

//   const onClick = (index) => {
//     navigate(`/guideDetail?id=${index}`);
//     }

  const props3 = useSpring({
    transform: show ? "scale(1.03)" : "scale(1)",
    boxShadow: show
      ? "0 20px 25px rgb(0 0 0 / 25%)"
      : "0 2px 10px rgb(0 0 0 / 8%)"
  });

  return (
    <animated.div
      className={Styles.card}
      style={props3}
      onMouseEnter={() => setShown(true)}
      onMouseLeave={() => setShown(false)}
    //   onClick={onClick(item.id)}
    >
      <img src={item.thumbnail_img_url} alt="" />
    </animated.div>
  );
}

export default Card;
