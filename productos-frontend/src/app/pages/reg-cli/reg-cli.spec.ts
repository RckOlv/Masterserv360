import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegCli } from './reg-cli';

describe('RegCli', () => {
  let component: RegCli;
  let fixture: ComponentFixture<RegCli>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegCli]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegCli);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
