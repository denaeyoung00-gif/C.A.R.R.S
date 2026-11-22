Setting up C.A.R.R.S on your laptop (about 15 minutes)

A. Get the code

Install Git, IntelliJ, a JDK (21 or newer) and MySQL 8 with Workbench, if you haven’t already.
Open Git Bash and run:
cd ~/IdeaProjects
git clone https://github.com/denaeyoung00-gif/C.A.R.R.S.git
cd C.A.R.R.S
git checkout -b YOUR-FEATURE-NAME

Use a clone, not a downloaded zip. A zip can’t push or pull.

B. Set up IntelliJ

File > Open > choose the C.A.R.R.S folder.
Right-click the src folder > Mark Directory as > Sources Root.
File > Project Structure > Project: pick your JDK.
Project Structure > Libraries > + > Java: add the MySQL Connector/J jar, junit-4.13.2.jar and hamcrest-core-1.3.jar.
Build > Rebuild Project. There should be no red.

C. Set up the database

In Workbench, open and run sql/schema.sql, then sql/demo_data.sql, in that order.
In the project root, copy db.properties.example to a new file named db.properties. Put your own MySQL password in it. Never commit this file.
Run CarrsApp (in src/carrs/app). You should see the tabs. Go to Payments & Benefits and search “Jane” to check it works.

D. Plug in your feature

Make a class like RegisterApplicantTab implements FeatureTab (copy the pattern in PaymentTab.java). It needs getTitle(), getView() and onUserChanged().
In CarrsApp.registerTabs(), replace your PlaceholderTab line with features.add(new RegisterApplicantTab(repo));.
Read and write data only through CarrsRepository. If you need a new query, add a method to that interface and to MySqlCarrsRepository, with the SQL there.
Need new tables? Add them to schema.sql and your sample rows to demo_data.sql.

E. Share your work

git add .
git commit -m "Describe what you did"
git push origin YOUR-FEATURE-NAME

Then open a pull request into main on GitHub. Before you do, run git fetch origin and git merge origin/main to pick up other people’s changes. After anyone merges to main, everyone runs git pull origin main, and re-runs both SQL files if the database changed.
